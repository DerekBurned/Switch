package com.example.messenger.data.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.messenger.data.remote.call.ActiveCallHolder
import com.example.messenger.domain.repository.IClipRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import androidx.media3.common.AudioAttributes as PlayerAudioAttributes

const val CLIP_PROGRESS_INTERVAL_MS = 100

data class ClipQueueItem(
    val messageId: String,
    val conversationId: String,
    val durationMs: Long = 0L,
    val isVideo: Boolean = false,
)

data class ClipPlaybackState(
    val messageId: String? = null,
    val conversationId: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val speed: Float = 1f,
    val failedMessageId: String? = null,
    val isVideo: Boolean = false,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
) {
    val progress: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

internal enum class ClipToggle { PLAY, PAUSE, RESUME, IGNORE }

internal fun clipToggle(state: ClipPlaybackState, loadedMessageId: String?, target: String): ClipToggle = when {
    state.messageId != target -> ClipToggle.PLAY
    loadedMessageId == target -> if (state.isPlaying) ClipToggle.PAUSE else ClipToggle.RESUME
    state.isLoading -> ClipToggle.IGNORE
    else -> ClipToggle.PLAY
}

internal class VideoViewStack<T : Any> {
    private val views = mutableListOf<T>()

    val current: T? get() = views.lastOrNull()

    fun attach(view: T) {
        views.remove(view)
        views.add(view)
    }

    fun detach(view: T) {
        views.remove(view)
    }
}

internal fun shouldAutoAdvance(finished: ClipPlaybackState): Boolean = !finished.isVideo

internal enum class FocusReaction { PAUSE_AND_RELEASE, PAUSE_AND_HOLD, RESUME, NONE }

internal fun focusReaction(change: Int, resumeOnGain: Boolean): FocusReaction = when (change) {
    AudioManager.AUDIOFOCUS_LOSS -> FocusReaction.PAUSE_AND_RELEASE
    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
    -> FocusReaction.PAUSE_AND_HOLD
    AudioManager.AUDIOFOCUS_GAIN -> if (resumeOnGain) FocusReaction.RESUME else FocusReaction.NONE
    else -> FocusReaction.NONE
}

@OptIn(UnstableApi::class)
@Singleton
class ClipPlaybackController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clipRepository: IClipRepository,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(ClipPlaybackState())
    val state: StateFlow<ClipPlaybackState> = _state.asStateFlow()

    private var player: ExoPlayer? = null
    private var loadedMessageId: String? = null
    private var progressJob: Job? = null
    private var prepareJob: Job? = null
    private var advanceJob: Job? = null
    private var speed = 1f
    private val videoViews = VideoViewStack<TextureView>()
    private var holdsFocus = false
    private var resumeOnGain = false

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val focusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setOnAudioFocusChangeListener(::onFocusChange)
            .build()
    }

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (loadedMessageId == null) return
            when (playbackState) {
                Player.STATE_READY -> {
                    val duration = player?.duration ?: C.TIME_UNSET
                    _state.value = if (duration != C.TIME_UNSET && duration > 0) {
                        _state.value.copy(durationMs = duration, isLoading = false)
                    } else {
                        _state.value.copy(isLoading = false)
                    }
                }
                Player.STATE_ENDED -> onCompleted()
                else -> Unit
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (loadedMessageId == null) return
            _state.value = _state.value.copy(isPlaying = isPlaying)
            if (isPlaying) startProgressUpdates() else stopProgressUpdates()
        }

        override fun onPlayerError(error: PlaybackException) {
            val failed = loadedMessageId ?: return
            Log.w(TAG, "clip playback failed for $failed", error)
            clipRepository.evictCachedCopy(failed)
            fail(failed)
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (loadedMessageId == null || videoSize.width <= 0 || videoSize.height <= 0) return
            _state.value = _state.value.copy(
                videoWidth = (videoSize.width * videoSize.pixelWidthHeightRatio).toInt(),
                videoHeight = videoSize.height,
            )
        }
    }

    init {
        scope.launch {
            ActiveCallHolder.state
                .map { it != null }
                .distinctUntilChanged()
                .collect { inCall -> if (inCall) pause() }
        }
        scope.launch {
            clipRepository.observeDeletedClipIds().collect { deleted ->
                if (_state.value.messageId in deleted) stop()
            }
        }
    }

    fun toggle(item: ClipQueueItem) {
        when (clipToggle(_state.value, loadedMessageId, item.messageId)) {
            ClipToggle.PLAY -> play(item)
            ClipToggle.PAUSE -> pause()
            ClipToggle.RESUME -> resume()
            ClipToggle.IGNORE -> Unit
        }
    }

    fun pause() {
        resumeOnGain = false
        player?.pause()
    }

    fun resume() {
        val active = player ?: return
        if (!holdsLoadedClip()) return
        if (ActiveCallHolder.snapshot() != null) return
        if (!requestFocus()) return
        active.play()
    }

    fun seek(fraction: Float) {
        val active = player ?: return
        if (!holdsLoadedClip()) return
        val duration = _state.value.durationMs
        if (duration <= 0L) return
        val target = (duration * fraction.coerceIn(0f, 1f)).toLong()
        active.seekTo(target)
        _state.value = _state.value.copy(positionMs = target)
    }

    fun attachVideo(view: TextureView) {
        videoViews.attach(view)
        player?.setVideoTextureView(view)
    }

    fun detachVideo(view: TextureView) {
        val wasShown = videoViews.current === view
        videoViews.detach(view)
        if (!wasShown) return
        player?.clearVideoTextureView(view)
        videoViews.current?.let { player?.setVideoTextureView(it) }
    }

    fun setSpeed(value: Float) {
        speed = value
        player?.playbackParameters = PlaybackParameters(value)
        _state.value = _state.value.copy(speed = value)
    }

    fun stop() {
        prepareJob?.cancel()
        advanceJob?.cancel()
        unload()
        player?.release()
        player = null
        abandonFocus()
        _state.value = ClipPlaybackState(speed = speed)
    }

    private fun play(item: ClipQueueItem) {
        if (ActiveCallHolder.snapshot() != null) return
        prepareJob?.cancel()
        advanceJob?.cancel()
        unload()
        _state.value = ClipPlaybackState(
            messageId = item.messageId,
            conversationId = item.conversationId,
            durationMs = item.durationMs,
            isLoading = true,
            speed = speed,
            isVideo = item.isVideo,
        )
        prepareJob = scope.launch {
            val file = clipRepository.ensureLocal(item.messageId)
            if (_state.value.messageId != item.messageId) return@launch
            if (file == null) {
                fail(item.messageId)
                return@launch
            }
            if (!requestFocus()) {
                _state.value = ClipPlaybackState(speed = speed)
                return@launch
            }
            val active = player ?: ExoPlayer.Builder(context)
                .setAudioAttributes(SPEECH_ATTRIBUTES, false)
                .setHandleAudioBecomingNoisy(true)
                .build()
                .also {
                    it.addListener(listener)
                    player = it
                }
            videoViews.current?.let(active::setVideoTextureView)
            active.setMediaItem(MediaItem.fromUri(file.toURI().toString()))
            active.playbackParameters = PlaybackParameters(speed)
            loadedMessageId = item.messageId
            resumeOnGain = false
            active.prepare()
            active.play()
            prefetchNext(item.messageId)
        }
    }

    private fun unload() {
        stopProgressUpdates()
        loadedMessageId = null
        player?.stop()
        player?.clearMediaItems()
    }

    private fun fail(messageId: String) {
        unload()
        abandonFocus()
        _state.value = ClipPlaybackState(speed = speed, failedMessageId = messageId)
    }

    private fun holdsLoadedClip(): Boolean =
        loadedMessageId != null && loadedMessageId == _state.value.messageId

    private fun prefetchNext(afterMessageId: String) {
        scope.launch {
            val next = clipRepository.nextVoiceClip(afterMessageId) ?: return@launch
            clipRepository.ensureLocal(next.id)
        }
    }

    private fun onCompleted() {
        val finished = loadedMessageId ?: return
        stopProgressUpdates()
        advanceJob?.cancel()
        advanceJob = scope.launch {
            clipRepository.markPlayed(finished)
            if (!shouldAutoAdvance(_state.value)) {
                if (_state.value.messageId == finished) stop()
                return@launch
            }
            val next = clipRepository.nextVoiceClip(finished)
            if (_state.value.messageId != finished) return@launch
            if (next == null) {
                stop()
            } else {
                play(ClipQueueItem(next.id, next.conversationId, next.clip.durationMs))
            }
        }
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressJob = scope.launch {
            while (isActive) {
                val active = player ?: return@launch
                val duration = active.duration.takeIf { it != C.TIME_UNSET && it > 0 }
                    ?: _state.value.durationMs
                _state.value = _state.value.copy(
                    positionMs = active.currentPosition,
                    durationMs = duration,
                )
                delay(CLIP_PROGRESS_INTERVAL_MS.milliseconds)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun onFocusChange(change: Int) {
        when (focusReaction(change, resumeOnGain)) {
            FocusReaction.PAUSE_AND_RELEASE -> {
                resumeOnGain = false
                player?.pause()
                abandonFocus()
            }
            FocusReaction.PAUSE_AND_HOLD -> {
                resumeOnGain = player?.isPlaying == true
                player?.pause()
            }
            FocusReaction.RESUME -> {
                resumeOnGain = false
                if (holdsLoadedClip()) player?.play()
            }
            FocusReaction.NONE -> Unit
        }
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

    private companion object {
        const val TAG = "ClipPlaybackController"
        val SPEECH_ATTRIBUTES: PlayerAudioAttributes = PlayerAudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()
    }
}
