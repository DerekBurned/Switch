package com.example.messenger.presentation.screens.chat

import android.graphics.Matrix
import android.view.TextureView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import com.example.messenger.R
import com.example.messenger.data.media.CLIP_PROGRESS_INTERVAL_MS
import com.example.messenger.data.media.ClipPlaybackState
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.model.MediaTransfer
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.voicemessage.Clip
import com.example.messenger.presentation.components.chat.ReplyQuote
import com.example.messenger.presentation.components.common.MessageStatusIcon
import com.example.messenger.presentation.featureflag.featureEnabled
import com.example.messenger.presentation.theme.BubbleSent
import com.example.messenger.presentation.theme.BubbleSentTop
import com.example.messenger.presentation.theme.messengerTokens
import com.example.messenger.util.DateUtils
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

internal fun ringFraction(x: Float, y: Float, cx: Float, cy: Float): Float {
    val turns = atan2(x - cx, -(y - cy)) / (2f * PI.toFloat())
    return (turns + 1f) % 1f
}

internal fun isOnRing(x: Float, y: Float, cx: Float, cy: Float, radius: Float, bandPx: Float): Boolean =
    abs(hypot(x - cx, y - cy) - radius) <= bandPx

internal fun centerCropScale(videoW: Int, videoH: Int, viewW: Int, viewH: Int): Pair<Float, Float> {
    if (videoW <= 0 || videoH <= 0 || viewW <= 0 || viewH <= 0) return 1f to 1f
    val scale = max(viewW.toFloat() / videoW, viewH.toFloat() / videoH)
    return (videoW * scale / viewW) to (videoH * scale / viewH)
}

@Composable
internal fun VideoNoteBubble(
    clip: Clip.Video,
    messageId: String,
    isMe: Boolean,
    played: Boolean,
    status: MessageStatus,
    timestamp: Long,
    playback: State<ClipPlaybackState>,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    videoFrame: suspend (String) -> File?,
    onAttachVideo: (TextureView) -> Unit,
    onDetachVideo: (TextureView) -> Unit,
    transfer: MediaTransfer? = null,
    onRetrySend: () -> Unit = {},
    onCancelSend: () -> Unit = {},
    replySenderLabel: String? = null,
    replyText: String? = null,
    onReplyClick: () -> Unit = {},
    interactive: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val tokens = messengerTokens
    val playFailed by remember(messageId) {
        derivedStateOf { playback.value.failedMessageId == messageId }
    }
    val isActive by remember(messageId) {
        derivedStateOf { playback.value.messageId == messageId }
    }
    val isPlaying by remember(messageId) {
        derivedStateOf { playback.value.let { it.messageId == messageId && it.isPlaying } }
    }
    val isLoading by remember(messageId) {
        derivedStateOf { playback.value.let { it.messageId == messageId && it.isLoading } }
    }
    val positionSeconds by remember(messageId) {
        derivedStateOf {
            val current = playback.value
            if (current.messageId == messageId) (current.positionMs / 1000L).toInt() else -1
        }
    }
    val targetProgress by remember(messageId) {
        derivedStateOf {
            val current = playback.value
            if (current.messageId == messageId) current.progress else 0f
        }
    }
    val videoSize by remember(messageId) {
        derivedStateOf { playback.value.let { it.videoWidth to it.videoHeight } }
    }
    val animatedProgress = animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(CLIP_PROGRESS_INTERVAL_MS, easing = LinearEasing),
        label = "videoNoteProgress",
    )
    val diameter by animateDpAsState(
        targetValue = if (isActive) ACTIVE_DIAMETER else IDLE_DIAMETER,
        label = "videoNoteDiameter",
    )
    val frame by produceState<File?>(initialValue = null, messageId, transfer == null, isActive) {
        value = videoFrame(messageId)
    }
    val transferProgress = when (transfer) {
        is MediaTransfer.Uploading -> transfer.progress
        is MediaTransfer.Downloading -> transfer.progress
        else -> null
    }
    val button = voiceButton(transfer, status, isMe, isLoading, playFailed, isPlaying)
    val playbackEnabled = featureEnabled(FeatureFlag.CLIP_PLAYBACK)
    val seekEnabled = featureEnabled(FeatureFlag.CLIP_SEEK)
    val playHidden = !playbackEnabled && button in PLAY_START_BUTTONS
    val onTap: () -> Unit = when (button) {
        VoiceButton.CANCEL_SEND -> onCancelSend
        VoiceButton.RETRY_SEND -> onRetrySend
        VoiceButton.DOWNLOADING, VoiceButton.LOADING -> ({})
        VoiceButton.PAUSE -> onTogglePlay
        VoiceButton.RETRY_PLAY, VoiceButton.PLAY -> if (playbackEnabled) onTogglePlay else ({})
    }
    val tap by rememberUpdatedState(onTap)
    val seek by rememberUpdatedState(onSeek)
    val density = LocalDensity.current
    val ringBandPx = with(density) { RING_TOUCH_BAND.toPx() }
    val ringInsetPx = with(density) { (RING_INSET + RING_STROKE / 2).toPx() }
    val actionLabel = stringResource(if (isPlaying) R.string.video_note_pause else R.string.video_note_play)
    val placeholder = if (isMe) {
        Brush.linearGradient(listOf(BubbleSentTop, BubbleSent))
    } else {
        Brush.linearGradient(listOf(tokens.cardFill, tokens.cardFill))
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
    ) {
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            if (replySenderLabel != null) {
                val quoteShape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .widthIn(max = 240.dp)
                        .clip(quoteShape)
                        .background(tokens.cardFill, quoteShape)
                        .border(1.dp, tokens.panelBorder, quoteShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    ReplyQuote(
                        senderLabel = replySenderLabel,
                        text = replyText,
                        contentColor = tokens.textPrimary,
                        onClick = { if (interactive) onReplyClick() },
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(diameter)
                    .semantics {
                        contentDescription = actionLabel
                        role = Role.Button
                        if (interactive && !playHidden) {
                            onClick(label = actionLabel) {
                                tap()
                                true
                            }
                        }
                    }
                    .pointerInput(interactive, isActive, seekEnabled) {
                        if (!interactive) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val radius = size.width / 2f - ringInsetPx
                            val onRing = isActive && seekEnabled &&
                                isOnRing(down.position.x, down.position.y, cx, cy, radius, ringBandPx)
                            if (onRing) {
                                down.consume()
                                seek(ringFraction(down.position.x, down.position.y, cx, cy))
                                while (true) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                    change.consume()
                                    if (!change.pressed) break
                                    seek(ringFraction(change.position.x, change.position.y, cx, cy))
                                }
                            } else {
                                val up = waitForUpOrCancellation()
                                if (up != null && up.uptimeMillis - down.uptimeMillis < viewConfiguration.longPressTimeoutMillis) {
                                    up.consume()
                                    tap()
                                }
                            }
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(placeholder),
                ) {
                    val thumbnail = frame
                    if (thumbnail != null) {
                        AsyncImage(
                            model = thumbnail,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                    if (interactive && isActive && !isLoading) {
                        VideoNoteSurface(
                            videoWidth = videoSize.first,
                            videoHeight = videoSize.second,
                            onAttach = onAttachVideo,
                            onDetach = onDetachVideo,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                }
                Canvas(modifier = Modifier.matchParentSize()) {
                    val stroke = RING_STROKE.toPx()
                    val inset = RING_INSET.toPx() + stroke / 2f
                    val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                    val topLeft = Offset(inset, inset)
                    drawArc(
                        color = Color.White.copy(alpha = 0.3f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke),
                    )
                    val progress = transferProgress ?: if (isActive) animatedProgress.value else null
                    if (progress != null && progress > 0f) {
                        drawArc(
                            color = if (transferProgress != null) tokens.accent else Color.White,
                            startAngle = -90f,
                            sweepAngle = 360f * progress.coerceIn(0f, 1f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                    if (isActive && transferProgress == null) {
                        val angle = animatedProgress.value * 2f * PI.toFloat() - PI.toFloat() / 2f
                        val radius = arcSize.width / 2f
                        drawCircle(
                            color = Color.White,
                            radius = KNOB_RADIUS.toPx(),
                            center = Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius),
                        )
                    }
                }
                if (!playHidden) {
                    VideoNoteControl(
                        button = button,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                NoteChip(modifier = Modifier.align(Alignment.BottomStart)) {
                    val seconds = if (isActive && positionSeconds >= 0) positionSeconds else (clip.durationMs / 1000L).toInt()
                    Text(text = DateUtils.formatDuration(seconds), color = Color.White, fontSize = 12.sp)
                    if (!isMe && !played) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(tokens.accent),
                        )
                    }
                }
                NoteChip(modifier = Modifier.align(Alignment.BottomEnd)) {
                    Text(text = DateUtils.formatMessageTime(timestamp), color = Color.White, fontSize = 11.sp)
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusIcon(status = status)
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoNoteControl(button: VoiceButton, modifier: Modifier = Modifier) {
    if (button == VoiceButton.PAUSE || button == VoiceButton.DOWNLOADING) return
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        when (button) {
            VoiceButton.LOADING -> CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
            VoiceButton.CANCEL_SEND -> Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.voice_cancel_send),
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
            VoiceButton.RETRY_SEND -> Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.voice_retry_send),
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
            VoiceButton.RETRY_PLAY -> Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = stringResource(R.string.voice_play_failed),
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
            else -> Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun NoteChip(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun VideoNoteSurface(
    videoWidth: Int,
    videoHeight: Int,
    onAttach: (TextureView) -> Unit,
    onDetach: (TextureView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val attach by rememberUpdatedState(onAttach)
    val detach by rememberUpdatedState(onDetach)
    val size = rememberUpdatedState(videoWidth to videoHeight)
    AndroidView(
        factory = { context ->
            TextureView(context).apply {
                isOpaque = false
                addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                    (view as TextureView).applyCenterCrop(size.value.first, size.value.second)
                }
                attach(this)
            }
        },
        update = { view -> view.applyCenterCrop(videoWidth, videoHeight) },
        onRelease = { view -> detach(view) },
        modifier = modifier,
    )
}

private fun TextureView.applyCenterCrop(videoWidth: Int, videoHeight: Int) {
    val (sx, sy) = centerCropScale(videoWidth, videoHeight, width, height)
    setTransform(Matrix().apply { setScale(sx, sy, width / 2f, height / 2f) })
}

private val IDLE_DIAMETER = 200.dp
private val ACTIVE_DIAMETER = 264.dp
private val RING_STROKE = 3.dp
private val RING_INSET = 3.dp
private val RING_TOUCH_BAND = 28.dp
private val KNOB_RADIUS = 6.dp
