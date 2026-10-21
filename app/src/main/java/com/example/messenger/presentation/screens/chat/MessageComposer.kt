package com.example.messenger.presentation.screens.chat

import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.R
import com.example.messenger.data.media.VideoRecordState
import com.example.messenger.data.media.VoiceRecordState
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.voicemessage.Clip
import com.example.messenger.domain.model.voicemessage.ClipKind
import com.example.messenger.presentation.components.chat.AttachmentBar
import com.example.messenger.presentation.components.chat.CANCEL_THRESHOLD
import com.example.messenger.presentation.components.chat.LOCK_THRESHOLD
import com.example.messenger.presentation.components.chat.RecordLockRail
import com.example.messenger.presentation.components.chat.RecordingBar
import com.example.messenger.presentation.components.chat.VoiceDraftBar
import com.example.messenger.presentation.components.chat.dragProgress
import com.example.messenger.presentation.components.chat.recordGesture
import com.example.messenger.presentation.components.common.KeepScreenOn
import com.example.messenger.presentation.featureflag.featureEnabled
import com.example.messenger.presentation.theme.messengerTokens
import kotlinx.coroutines.delay

@Composable
internal fun MessageComposer(
    messageText: String,
    uiState: ChatUiState,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    onClearAttachments: () -> Unit,
    onClearReply: () -> Unit,
    onHeightChanged: (Int) -> Unit,
    onStartRecording: () -> Unit,
    onLockRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onFinishRecording: () -> Unit,
    onInterruptRecording: () -> Unit,
    onSendVoiceDraft: () -> Unit,
    onDiscardVoiceDraft: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleClipMode: () -> Unit = {},
) {
    val tokens = messengerTokens
    val voiceRecording = uiState.recording as? VoiceRecordState.Recording
    val videoRecording = uiState.videoRecording as? VideoRecordState.Recording
    val recordingActive = voiceRecording != null || videoRecording != null
    KeepScreenOn(enabled = recordingActive)
    if (uiState.partnerDeleted) {
        DeletedPartnerNotice(modifier = modifier.onSizeChanged { onHeightChanged(it.height) })
        return
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { onHeightChanged(it.height) },
    ) {
        AttachmentBar(
            attachments = uiState.pendingAttachments,
            onClear = onClearAttachments,
            modifier = Modifier.fillMaxWidth(),
        )

        val replyingTo = uiState.replyingTo
        if (replyingTo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tokens.fieldFill)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = null,
                    tint = tokens.accent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (replyingTo) {
                        is Message.Text  -> replyingTo.text
                        is Message.Media -> replyingTo.caption.ifBlank { "Media" }
                        is Message.Call  -> "Call"
                        is Message.Clip  -> when (replyingTo.clip) {
                            is Clip.Voice -> stringResource(R.string.clip_voice_message)
                            is Clip.Video -> stringResource(R.string.clip_video_message)
                        }
                    },
                    color = tokens.textOnField,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onClearReply,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel reply",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        val composerShape = RoundedCornerShape(28.dp)
        val locked = voiceRecording?.locked == true || videoRecording?.locked == true
        val draft = uiState.clipDraft.takeIf { !recordingActive }
        var recordDrag by remember { mutableStateOf(Offset.Zero) }
        var modeHint by remember { mutableStateOf<ClipKind?>(null) }
        LaunchedEffect(modeHint) {
            if (modeHint != null) {
                delay(MODE_HINT_MS)
                modeHint = null
            }
        }
        val density = LocalDensity.current
        val lockPx = with(density) { LOCK_THRESHOLD.toPx() }
        val cancelPx = with(density) { CANCEL_THRESHOLD.toPx() }
        Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .shadow(elevation = 10.dp, shape = composerShape, clip = false)
                .clip(composerShape)
                .background(tokens.cardFill)
                .border(1.dp, tokens.panelBorder, composerShape)
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (recordingActive) {
                RecordingBar(
                    elapsedMs = voiceRecording?.elapsedMs ?: videoRecording?.elapsedMs ?: 0L,
                    amplitudes = voiceRecording?.amplitudes,
                    sampleCount = voiceRecording?.sampleCount ?: 0,
                    locked = locked,
                    onCancel = onCancelRecording,
                    onStop = onFinishRecording,
                    dragX = { recordDrag.x },
                    cancelProgress = { dragProgress(-recordDrag.x, cancelPx) },
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                )
            } else if (draft != null) {
                VoiceDraftBar(
                    durationMs = draft.durationMs,
                    waveform = draft.waveform,
                    onDiscard = onDiscardVoiceDraft,
                    kind = draft.kind,
                    file = draft.file,
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                )
            } else {
                if (featureEnabled(FeatureFlag.MEDIA_SENDING)) {
                    IconButton(
                        onClick = onAttachmentClick,
                        modifier = Modifier.size(42.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Add attachment",
                            tint = tokens.accent,
                            modifier = Modifier.size(26.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(tokens.fieldFill)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (messageText.isEmpty()) {
                        Text(
                            text = "Message",
                            color = tokens.textOnField.copy(alpha = 0.45f),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    BasicTextField(
                        value = messageText,
                        onValueChange = onMessageTextChange,
                        cursorBrush = SolidColor(tokens.accent),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = tokens.textOnField),
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            val hasContent = messageText.isNotBlank() || uiState.pendingAttachments.isNotEmpty()
            val voiceEnabled = featureEnabled(FeatureFlag.VOICE_MESSAGES)
            val videoNotesEnabled = featureEnabled(FeatureFlag.VIDEO_NOTES)
            val clipMode = availableClipMode(uiState.clipMode, voiceEnabled, videoNotesEnabled)
            LaunchedEffect(clipMode, uiState.clipMode, recordingActive) {
                if (clipMode != null && clipMode != uiState.clipMode && !recordingActive) onToggleClipMode()
            }
            val actionModifier = when {
                locked -> Modifier.clickable { onFinishRecording() }
                draft != null -> Modifier.clickable { onSendVoiceDraft() }
                hasContent -> Modifier.clickable(enabled = !uiState.isSending) { onSendClick() }
                clipMode == null -> Modifier
                else -> Modifier.recordGesture(
                    enabled = !uiState.isSending,
                    onTap = {
                        if (voiceEnabled && videoNotesEnabled) {
                            onToggleClipMode()
                            modeHint = clipMode.toggled()
                        }
                    },
                    onStart = {
                        modeHint = null
                        onStartRecording()
                    },
                    onLock = onLockRecording,
                    onCancel = onCancelRecording,
                    onFinish = onFinishRecording,
                    onInterrupt = onInterruptRecording,
                    onDrag = { recordDrag = it },
                    lockEnabled = featureEnabled(FeatureFlag.RECORDING_LOCK),
                )
            }
            val actionIcon = when {
                hasContent || locked || draft != null || clipMode == null -> ComposerAction.SEND
                clipMode == ClipKind.VIDEO -> ComposerAction.VIDEO
                else -> ComposerAction.VOICE
            }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (recordingActive && !locked) tokens.danger else tokens.accent)
                    .then(actionModifier),
                contentAlignment = Alignment.Center,
            ) {
                val description = when {
                    locked && videoRecording != null -> stringResource(R.string.video_note_send)
                    locked -> stringResource(R.string.voice_send)
                    draft != null && draft.kind == ClipKind.VIDEO -> stringResource(R.string.video_send_draft)
                    draft != null -> stringResource(R.string.voice_send_draft)
                    hasContent || clipMode == null -> "Send"
                    clipMode == ClipKind.VIDEO -> stringResource(R.string.video_hold_to_record)
                    else -> stringResource(R.string.voice_hold_to_record)
                }
                Crossfade(targetState = actionIcon, label = "composerAction") { action ->
                    Icon(
                        imageVector = when (action) {
                            ComposerAction.SEND -> Icons.AutoMirrored.Outlined.Send
                            ComposerAction.VIDEO -> Icons.Default.Videocam
                            ComposerAction.VOICE -> Icons.Default.Mic
                        },
                        contentDescription = description,
                        tint = tokens.onAccent,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = recordingActive && !locked && featureEnabled(FeatureFlag.RECORDING_LOCK),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .floatAboveEnd(endInset = RAIL_END_INSET, gap = 4.dp),
        ) {
            RecordLockRail(progress = { dragProgress(-recordDrag.y, lockPx) })
        }
        val hint = modeHint
        androidx.compose.animation.AnimatedVisibility(
            visible = hint != null && !recordingActive,
            enter = fadeIn() + slideInVertically { it / 3 },
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .floatAboveEnd(endInset = 10.dp, gap = 2.dp),
        ) {
            Text(
                text = stringResource(if (hint == ClipKind.VIDEO) R.string.video_mode_hint else R.string.voice_mode_hint),
                color = tokens.textPrimary,
                fontSize = 13.sp,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(14.dp), clip = false)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.backgroundBottom)
                    .background(tokens.cardFill)
                    .border(1.dp, tokens.panelBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        }
    }
}

@Composable
private fun DeletedPartnerNotice(modifier: Modifier = Modifier) {
    val tokens = messengerTokens
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(tokens.fieldFill)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "This account no longer exists. You can read this conversation, but not reply.",
            color = tokens.textMuted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

private enum class ComposerAction { SEND, VOICE, VIDEO }

internal fun availableClipMode(preferred: ClipKind, voiceEnabled: Boolean, videoNotesEnabled: Boolean): ClipKind? =
    when {
        preferred == ClipKind.VIDEO && videoNotesEnabled -> ClipKind.VIDEO
        preferred == ClipKind.VOICE && voiceEnabled -> ClipKind.VOICE
        voiceEnabled -> ClipKind.VOICE
        videoNotesEnabled -> ClipKind.VIDEO
        else -> null
    }

private fun Modifier.floatAboveEnd(endInset: Dp, gap: Dp): Modifier = layout { measurable, _ ->
    val placeable = measurable.measure(Constraints())
    layout(0, 0) {
        placeable.place(
            x = -placeable.width - endInset.roundToPx(),
            y = -placeable.height - gap.roundToPx(),
        )
    }
}

private val RAIL_END_INSET = 19.dp
private const val MODE_HINT_MS = 2_000L
