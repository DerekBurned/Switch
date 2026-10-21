package com.example.messenger.data.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.messenger.data.remote.call.ActiveCallHolder
import com.example.messenger.domain.model.voicemessage.Clip
import com.example.messenger.domain.model.voicemessage.ClipKind
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.roundToInt

sealed interface VoiceRecordState {
    data object Idle : VoiceRecordState

    data class Recording(
        val conversationId: String,
        val elapsedMs: Long = 0L,
        val amplitudes: List<Int> = emptyList(),
        val sampleCount: Int = 0,
        val locked: Boolean = false,
    ) : VoiceRecordState
}

fun VoiceRecordState.visibleTo(conversationId: String): VoiceRecordState = when (this) {
    is VoiceRecordState.Recording -> if (this.conversationId == conversationId) this else VoiceRecordState.Idle
    VoiceRecordState.Idle -> this
}

data class ClipRecording(
    val conversationId: String,
    val file: File,
    val durationMs: Long,
    val waveform: List<Int> = emptyList(),
    val kind: ClipKind = ClipKind.VOICE,
)

const val WAVEFORM_BUCKETS = 50

const val VOICE_SAMPLE_INTERVAL_MS = 100L

fun downsampleWaveform(samples: List<Int>, buckets: Int = WAVEFORM_BUCKETS): List<Int> {
    if (buckets <= 0) return emptyList()
    if (samples.isEmpty()) return List(buckets) { 0 }
    return List(buckets) { bucket ->
        val start = bucket * samples.size / buckets
        val end = (((bucket + 1) * samples.size / buckets).coerceAtLeast(start + 1))
            .coerceAtMost(samples.size)
        samples.subList(start, end).average().toInt()
    }
}

private const val FULL_SCALE_AMPLITUDE = 32_767.0
private const val SILENCE_FLOOR_DB = -50.0

internal fun amplitudeToLevel(amplitude: Int): Int {
    if (amplitude <= 0) return 0
    val db = 20.0 * log10(amplitude / FULL_SCALE_AMPLITUDE)
    return ((db - SILENCE_FLOOR_DB) / -SILENCE_FLOOR_DB * 100.0).roundToInt().coerceIn(0, 100)
}

fun normalizeWaveform(levels: List<Int>): List<Int> {
    val peak = levels.maxOrNull() ?: return levels
    if (peak <= 0 || peak >= 100) return levels.map { it.coerceIn(0, 100) }
    return levels.map { (it.coerceAtLeast(0) * 100f / peak).roundToInt() }
}

internal fun interruptsRecording(change: Int): Boolean =
    change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT

internal fun recordedDurationMs(startedAt: Long, stoppedAt: Long): Long =
    (stoppedAt - startedAt).coerceIn(0L, Clip.Voice.MAX_DURATION_MS)

@Singleton
class VoiceRecorder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaCache: MediaCache,
    private val drafts: VoiceDraftStore,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<VoiceRecordState>(VoiceRecordState.Idle)
    val state: StateFlow<VoiceRecordState> = _state.asStateFlow()

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var owner: String? = null
    private var startedAt = 0L
    private var pollJob: Job? = null
    private val samples = mutableListOf<Int>()
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
        if (recorder != null) return false
        if (ActiveCallHolder.snapshot() != null) return false
        if (!requestFocus()) return false
        val file = mediaCache.newRecordingFile(VOICE_EXTENSION)
        val created = buildRecorder()
        val started = runCatching {
            created.setAudioSource(MediaRecorder.AudioSource.MIC)
            created.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            created.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            created.setAudioChannels(CHANNELS)
            created.setAudioSamplingRate(SAMPLE_RATE)
            created.setAudioEncodingBitRate(BIT_RATE)
            created.setOutputFile(file.absolutePath)
            created.setMaxDuration(Clip.Voice.MAX_DURATION_MS.toInt())
            created.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) interrupt()
            }
            created.setOnErrorListener { _, what, extra ->
                Log.w(TAG, "voice recorder error what=$what extra=$extra")
                cancel()
            }
            created.prepare()
            created.start()
        }
        if (started.isFailure) {
            Log.w(TAG, "voice recording failed to start", started.exceptionOrNull())
            runCatching { created.release() }
            file.delete()
            abandonFocus()
            return false
        }
        recorder = created
        outputFile = file
        owner = conversationId
        startedAt = SystemClock.elapsedRealtime()
        samples.clear()
        _state.value = VoiceRecordState.Recording(conversationId = conversationId)
        pollJob = scope.launch { pollAmplitude() }
        return true
    }

    fun lock(conversationId: String) {
        if (owner != conversationId) return
        _state.update { current ->
            if (current is VoiceRecordState.Recording) current.copy(locked = true) else current
        }
    }

    fun stop(conversationId: String): ClipRecording? {
        if (owner != conversationId) return null
        return finish()
    }

    fun cancel(conversationId: String? = null) {
        if (conversationId != null && owner != conversationId) return
        val file = outputFile
        release(recorder)
        file?.delete()
    }

    fun interrupt(conversationId: String? = null) {
        if (recorder == null) return
        if (conversationId != null && owner != conversationId) return
        finish()?.let(drafts::save)
    }

    private fun finish(): ClipRecording? {
        val active = recorder
        val file = outputFile
        val conversationId = owner
        val durationMs = recordedDurationMs(startedAt, SystemClock.elapsedRealtime())
        val waveform = normalizeWaveform(downsampleWaveform(samples.toList()))
        release(active)
        if (active == null || file == null || conversationId == null) return null
        if (!file.exists() || file.length() == 0L || durationMs < MIN_DURATION_MS) {
            file.delete()
            return null
        }
        return ClipRecording(
            conversationId = conversationId,
            file = file,
            durationMs = durationMs,
            waveform = waveform,
        )
    }

    private fun release(active: MediaRecorder?) {
        pollJob?.cancel()
        pollJob = null
        if (active != null) {
            runCatching { active.stop() }.onFailure { Log.w(TAG, "voice recorder stop failed", it) }
            runCatching { active.release() }
        }
        recorder = null
        outputFile = null
        owner = null
        abandonFocus()
        _state.value = VoiceRecordState.Idle
    }

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

    private suspend fun pollAmplitude() {
        while (currentCoroutineContext().isActive) {
            delay(VOICE_SAMPLE_INTERVAL_MS)
            val active = recorder ?: return
            val amplitude = runCatching { active.maxAmplitude }.getOrDefault(0)
            samples.add(amplitudeToLevel(amplitude))
            _state.update { current ->
                if (current is VoiceRecordState.Recording) {
                    current.copy(
                        elapsedMs = SystemClock.elapsedRealtime() - startedAt,
                        amplitudes = samples.takeLast(VISIBLE_AMPLITUDES),
                        sampleCount = samples.size,
                    )
                } else {
                    current
                }
            }
        }
    }

    private fun buildRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    private companion object {
        const val TAG = "VoiceRecorder"
        const val SAMPLE_RATE = 44_100
        const val BIT_RATE = 64_000
        const val CHANNELS = 1
        const val MIN_DURATION_MS = 500L
        const val VISIBLE_AMPLITUDES = 256
        const val VOICE_EXTENSION = Clip.Voice.FILE_EXTENSION
    }
}
