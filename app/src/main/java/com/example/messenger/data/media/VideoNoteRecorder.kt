package com.example.messenger.data.media

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.messenger.data.remote.call.ActiveCallHolder
import com.example.messenger.domain.model.voicemessage.Clip
import com.example.messenger.domain.model.voicemessage.ClipKind
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface VideoRecordState {
    data object Idle : VideoRecordState

    data class Recording(
        val conversationId: String,
        val elapsedMs: Long = 0L,
        val started: Boolean = false,
        val locked: Boolean = false,
    ) : VideoRecordState
}

fun VideoRecordState.visibleTo(conversationId: String): VideoRecordState = when (this) {
    is VideoRecordState.Recording -> if (this.conversationId == conversationId) this else VideoRecordState.Idle
    VideoRecordState.Idle -> this
}

internal enum class VideoEnd { SEND, DRAFT, DISCARD }

private const val MIN_VIDEO_DURATION_MS = 1_000L

internal fun nextVideoEnd(current: VideoEnd?, requested: VideoEnd): VideoEnd =
    if (current == null || requested == VideoEnd.DISCARD) requested else current

internal fun videoEndWithoutWaiter(current: VideoEnd?): VideoEnd? =
    if (current == VideoEnd.SEND) VideoEnd.DRAFT else current

private val USABLE_FINALIZE_ERRORS = setOf(
    VideoRecordEvent.Finalize.ERROR_NONE,
    VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED,
    VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED,
    VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE,
)

internal fun videoFinalizeOutcome(requested: VideoEnd?, error: Int, fileBytes: Long, durationMs: Long): VideoEnd = when {
    requested == VideoEnd.DISCARD -> VideoEnd.DISCARD
    fileBytes <= 0L || durationMs < MIN_VIDEO_DURATION_MS || error !in USABLE_FINALIZE_ERRORS -> VideoEnd.DISCARD
    else -> requested ?: VideoEnd.DRAFT
}

@Singleton
class VideoNoteRecorder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaCache: MediaCache,
    private val drafts: VoiceDraftStore,
) {

    private class Session(val conversationId: String, val file: File) {
        var bindJob: Job? = null
        var provider: ProcessCameraProvider? = null
        var preview: Preview? = null
        var capture: VideoCapture<Recorder>? = null
        var recording: Recording? = null
        var requested: VideoEnd? = null
        var durationMs: Long = 0L
        var unclaimed: ClipRecording? = null
        val result = CompletableDeferred<ClipRecording?>()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<VideoRecordState>(VideoRecordState.Idle)
    val state: StateFlow<VideoRecordState> = _state.asStateFlow()

    private var session: Session? = null
    private var surfaceProvider: Preview.SurfaceProvider? = null
    private var holdsFocus = false

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val focusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setOnAudioFocusChangeListener { change -> if (interruptsRecording(change)) interrupt() }
            .build()
    }

    init {
        scope.launch {
            ActiveCallHolder.state
                .map { it != null }
                .distinctUntilChanged()
                .filter { inCall -> inCall }
                .collect { interrupt() }
        }
        scope.launch {
            ProcessLifecycleOwner.get().lifecycle.currentStateFlow
                .map { it.isAtLeast(Lifecycle.State.STARTED) }
                .distinctUntilChanged()
                .filter { visible -> !visible }
                .collect { interrupt() }
        }
    }

    fun start(conversationId: String): Boolean {
        if (session != null) return false
        if (ActiveCallHolder.snapshot() != null) return false
        if (!granted(Manifest.permission.CAMERA) || !granted(Manifest.permission.RECORD_AUDIO)) return false
        if (!requestFocus()) return false
        val active = Session(conversationId, mediaCache.newRecordingFile(ClipKind.VIDEO.extension))
        session = active
        _state.value = VideoRecordState.Recording(conversationId = conversationId)
        active.bindJob = scope.launch { bindAndRecord(active) }
        return true
    }

    fun attachPreview(provider: Preview.SurfaceProvider) {
        surfaceProvider = provider
        session?.preview?.setSurfaceProvider(provider)
    }

    fun detachPreview(provider: Preview.SurfaceProvider) {
        if (surfaceProvider !== provider) return
        surfaceProvider = null
        session?.preview?.setSurfaceProvider(null)
    }

    fun lock(conversationId: String) {
        val active = session ?: return
        if (active.conversationId != conversationId) return
        updateRecording(active) { it.copy(locked = true) }
    }

    suspend fun stop(conversationId: String): ClipRecording? {
        val active = session ?: return null
        if (active.conversationId != conversationId) return null
        end(active, VideoEnd.SEND)
        var claimed: ClipRecording? = null
        try {
            claimed = withTimeoutOrNull(FINALIZE_TIMEOUT_MS) { active.result.await() }
            return claimed
        } finally {
            if (claimed == null) keepUnclaimedAsDraft(active) else active.unclaimed = null
        }
    }

    fun cancel(conversationId: String? = null) {
        val active = session ?: return
        if (conversationId != null && active.conversationId != conversationId) return
        end(active, VideoEnd.DISCARD)
    }

    fun interrupt(conversationId: String? = null) {
        val active = session ?: return
        if (conversationId != null && active.conversationId != conversationId) return
        end(active, VideoEnd.DRAFT)
    }

    @SuppressLint("MissingPermission")
    private suspend fun bindAndRecord(active: Session) {
        try {
            val provider = withContext(NonCancellable) { ProcessCameraProvider.awaitInstance(context) }
            if (session !== active) return
            val selector = if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(Quality.SD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)),
                )
                .setTargetVideoEncodingBitRate(VIDEO_BIT_RATE)
                .build()
            val capture = VideoCapture.Builder(recorder)
                .setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
                .build()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(surfaceProvider)
            provider.bindToLifecycle(ProcessLifecycleOwner.get(), selector, preview, capture)
            active.provider = provider
            active.preview = preview
            active.capture = capture
            val output = FileOutputOptions.Builder(active.file)
                .setDurationLimitMillis(Clip.Video.MAX_DURATION_MS)
                .setFileSizeLimit(MAX_FILE_BYTES)
                .build()
            active.recording = recorder.prepareRecording(context, output)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(context)) { event -> onEvent(active, event) }
        } catch (e: CancellationException) {
            if (!currentCoroutineContext().isActive) throw e
            Log.w(TAG, "camera provider was cancelled", e)
            complete(active, VideoRecordEvent.Finalize.ERROR_UNKNOWN)
        } catch (e: Exception) {
            Log.w(TAG, "video note recording failed to start", e)
            complete(active, VideoRecordEvent.Finalize.ERROR_UNKNOWN)
        }
    }

    private fun onEvent(active: Session, event: VideoRecordEvent) {
        when (event) {
            is VideoRecordEvent.Start -> updateRecording(active) { it.copy(started = true) }
            is VideoRecordEvent.Status -> {
                active.durationMs = event.recordingStats.recordedDurationNanos / NANOS_PER_MILLI
                updateRecording(active) { it.copy(elapsedMs = active.durationMs) }
            }
            is VideoRecordEvent.Finalize -> {
                active.durationMs = maxOf(active.durationMs, event.recordingStats.recordedDurationNanos / NANOS_PER_MILLI)
                if (event.hasError()) Log.w(TAG, "video note finalized with error ${event.error}", event.cause)
                complete(active, event.error)
            }
            else -> Unit
        }
    }

    private fun end(active: Session, requested: VideoEnd) {
        active.requested = nextVideoEnd(active.requested, requested)
        val recording = active.recording
        if (recording != null) {
            runCatching { recording.stop() }.onFailure { Log.w(TAG, "video note stop failed", it) }
        } else {
            complete(active, VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA)
        }
    }

    private fun complete(active: Session, error: Int) {
        if (active.result.isCompleted) return
        val fileBytes = if (active.file.exists()) active.file.length() else 0L
        val outcome = videoFinalizeOutcome(active.requested, error, fileBytes, active.durationMs)
        release(active)
        val recording = ClipRecording(
            conversationId = active.conversationId,
            file = active.file,
            durationMs = active.durationMs.coerceAtMost(Clip.Video.MAX_DURATION_MS),
            kind = ClipKind.VIDEO,
        )
        when (outcome) {
            VideoEnd.SEND -> {
                active.unclaimed = recording
                active.result.complete(recording)
            }
            VideoEnd.DRAFT -> {
                drafts.save(recording)
                active.result.complete(null)
            }
            VideoEnd.DISCARD -> {
                active.file.delete()
                active.result.complete(null)
            }
        }
    }

    private fun keepUnclaimedAsDraft(active: Session) {
        val unclaimed = active.unclaimed
        if (unclaimed != null) {
            active.unclaimed = null
            drafts.save(unclaimed)
        } else if (!active.result.isCompleted) {
            active.requested = videoEndWithoutWaiter(active.requested)
        }
    }

    private fun release(active: Session) {
        active.bindJob?.cancel()
        active.recording?.let { runCatching { it.close() } }
        val useCases = listOfNotNull(active.preview, active.capture)
        if (useCases.isNotEmpty()) {
            runCatching { active.provider?.unbind(*useCases.toTypedArray()) }
                .onFailure { Log.w(TAG, "video note unbind failed", it) }
        }
        if (session === active) {
            session = null
            abandonFocus()
            _state.value = VideoRecordState.Idle
        }
    }

    private fun updateRecording(active: Session, transform: (VideoRecordState.Recording) -> VideoRecordState.Recording) {
        if (session !== active) return
        _state.update { current ->
            if (current is VideoRecordState.Recording && current.conversationId == active.conversationId) {
                transform(current)
            } else {
                current
            }
        }
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun requestFocus(): Boolean {
        if (holdsFocus) return true
        holdsFocus = audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return holdsFocus
    }

    private fun abandonFocus() {
        if (!holdsFocus) return
        audioManager.abandonAudioFocusRequest(focusRequest)
        holdsFocus = false
    }

    private companion object {
        const val TAG = "VideoNoteRecorder"
        const val VIDEO_BIT_RATE = 1_200_000
        const val MAX_FILE_BYTES = 38L * 1024 * 1024
        const val FINALIZE_TIMEOUT_MS = 5_000L
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
