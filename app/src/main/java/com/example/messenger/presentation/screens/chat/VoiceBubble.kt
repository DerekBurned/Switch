package com.example.messenger.presentation.screens.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.R
import com.example.messenger.data.media.CLIP_PROGRESS_INTERVAL_MS
import com.example.messenger.data.media.ClipPlaybackState
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.model.MediaTransfer
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.voicemessage.Clip
import com.example.messenger.presentation.components.chat.ReplyQuote
import com.example.messenger.presentation.components.chat.VoiceWaveform
import com.example.messenger.presentation.components.common.MessageStatusIcon
import com.example.messenger.presentation.featureflag.featureEnabled
import com.example.messenger.presentation.theme.BubbleSent
import com.example.messenger.presentation.theme.BubbleSentTop
import com.example.messenger.presentation.theme.messengerTokens
import com.example.messenger.util.DateUtils

internal enum class VoiceButton { CANCEL_SEND, DOWNLOADING, RETRY_SEND, LOADING, RETRY_PLAY, PAUSE, PLAY }

internal val PLAY_START_BUTTONS = setOf(VoiceButton.RETRY_PLAY, VoiceButton.PLAY)

internal fun voiceButton(
    transfer: MediaTransfer?,
    status: MessageStatus,
    isMe: Boolean,
    isLoading: Boolean,
    playFailed: Boolean,
    isPlaying: Boolean,
): VoiceButton = when {
    transfer is MediaTransfer.Uploading -> VoiceButton.CANCEL_SEND
    transfer is MediaTransfer.Downloading -> VoiceButton.DOWNLOADING
    isMe && status == MessageStatus.FAILED -> VoiceButton.RETRY_SEND
    isLoading -> VoiceButton.LOADING
    playFailed -> VoiceButton.RETRY_PLAY
    isPlaying -> VoiceButton.PAUSE
    else -> VoiceButton.PLAY
}

@Composable
internal fun VoiceBubble(
    clip: Clip.Voice,
    messageId: String,
    isMe: Boolean,
    played: Boolean,
    status: MessageStatus,
    timestamp: Long,
    playback: State<ClipPlaybackState>,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
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
    val targetProgress by remember(messageId) {
        derivedStateOf {
            val current = playback.value
            if (current.messageId == messageId) current.progress else 0f
        }
    }
    val animatedProgress = animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(CLIP_PROGRESS_INTERVAL_MS, easing = LinearEasing),
        label = "clipProgress",
    )
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
    ) {
        val bubbleShape = if (isMe) {
            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 5.dp, bottomStart = 18.dp)
        } else {
            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 5.dp)
        }
        val metaColor = if (isMe) Color.White.copy(alpha = 0.85f) else tokens.textMuted
        val bubbleModifier = if (isMe) {
            Modifier
                .width(260.dp)
                .clip(bubbleShape)
                .background(brush = Brush.linearGradient(listOf(BubbleSentTop, BubbleSent)), shape = bubbleShape)
        } else {
            Modifier
                .width(260.dp)
                .clip(bubbleShape)
                .background(color = tokens.cardFill, shape = bubbleShape)
                .border(width = 1.dp, color = tokens.panelBorder, shape = bubbleShape)
        }
        Column(modifier = bubbleModifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (replySenderLabel != null) {
                ReplyQuote(
                    senderLabel = replySenderLabel,
                    text = replyText,
                    contentColor = if (isMe) Color.White else tokens.textPrimary,
                    onClick = { if (interactive) onReplyClick() },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val button = voiceButton(transfer, status, isMe, isLoading, playFailed, isPlaying)
                val playbackEnabled = featureEnabled(FeatureFlag.CLIP_PLAYBACK)
                val onButtonClick: (() -> Unit)? = when (button) {
                    VoiceButton.CANCEL_SEND -> onCancelSend
                    VoiceButton.RETRY_SEND -> onRetrySend
                    VoiceButton.DOWNLOADING, VoiceButton.LOADING -> null
                    VoiceButton.RETRY_PLAY, VoiceButton.PAUSE, VoiceButton.PLAY -> onTogglePlay
                }
                if (playbackEnabled || button !in PLAY_START_BUTTONS) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isMe) Color.White.copy(alpha = 0.22f) else tokens.accent)
                            .then(
                                if (interactive && onButtonClick != null) Modifier.clickable { onButtonClick() }
                                else Modifier,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (button) {
                            VoiceButton.CANCEL_SEND -> {
                                CircularProgressIndicator(
                                    progress = { transfer?.progress ?: 0f },
                                    modifier = Modifier.size(34.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.voice_cancel_send),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            VoiceButton.DOWNLOADING -> CircularProgressIndicator(
                                progress = { transfer?.progress ?: 0f },
                                modifier = Modifier.size(34.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            VoiceButton.LOADING -> CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            VoiceButton.RETRY_SEND -> Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.voice_retry_send),
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                            VoiceButton.RETRY_PLAY -> Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = stringResource(R.string.voice_play_failed),
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                            VoiceButton.PAUSE, VoiceButton.PLAY -> Icon(
                                imageVector = if (button == VoiceButton.PAUSE) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = stringResource(
                                    if (button == VoiceButton.PAUSE) R.string.voice_pause else R.string.voice_play,
                                ),
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                VoiceWaveform(
                    waveform = clip.waveform,
                    progress = { animatedProgress.value },
                    playedColor = if (isMe) Color.White else tokens.accent,
                    idleColor = if (isMe) Color.White.copy(alpha = 0.35f) else tokens.textMuted.copy(alpha = 0.45f),
                    modifier = Modifier.weight(1f).height(32.dp),
                    onSeek = if (interactive && isActive && featureEnabled(FeatureFlag.CLIP_SEEK)) onSeek else null,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = DateUtils.formatDuration((clip.durationMs / 1000L).toInt()),
                    color = metaColor,
                    fontSize = 12.sp,
                )
                if (!isMe && !played) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(tokens.accent),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = DateUtils.formatMessageTime(timestamp),
                    color = metaColor,
                    fontSize = 11.sp,
                )
                if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    MessageStatusIcon(status = status)
                }
            }
        }
    }
}
