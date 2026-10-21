package com.example.messenger.presentation.screens.chat

import android.view.TextureView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.messenger.R
import com.example.messenger.data.media.ClipPlaybackState
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.call.CallType
import com.example.messenger.domain.model.voicemessage.Clip
import com.example.messenger.presentation.components.chat.ChatMessage
import com.example.messenger.presentation.components.chat.ChatTextBubble
import com.example.messenger.presentation.components.chat.EndedCallCard
import com.example.messenger.presentation.components.chat.LongPressMessage
import com.example.messenger.presentation.components.chat.MissedCallCard
import com.example.messenger.presentation.components.chat.SwipeToReply
import com.example.messenger.presentation.components.chat.model.MessageAction
import com.example.messenger.presentation.components.chat.model.MessageContextMenuState
import com.example.messenger.presentation.components.media.MediaAlbumGrid
import com.example.messenger.presentation.featureflag.featureEnabled
import com.example.messenger.presentation.theme.messengerTokens
import java.io.File

@Composable
internal fun MessageBubble(
    msg: Message,
    isMe: Boolean,
    uiState: ChatUiState,
    ctxState: MessageContextMenuState,
    onReply: (Message) -> Unit,
    onCopy: (String) -> Unit,
    onReplyClick: (messageId: String) -> Unit,
    onDelete: (Message, Boolean) -> Unit,
    onDownload: (MediaItem) -> Unit,
    onCancelUpload: (messageId: String, itemId: String) -> Unit,
    onCancelDownload: (itemId: String) -> Unit,
    onRetryMedia: (messageId: String) -> Unit,
    onCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onOpenMedia: (index: Int) -> Unit,
    onPlayClip: (String) -> Unit,
    onSeekClip: (Float) -> Unit,
    clipPlayback: State<ClipPlaybackState>,
    onRetryClip: (String) -> Unit,
    onCancelClipSend: (String) -> Unit,
    videoFrame: suspend (String) -> File? = { null },
    onAttachVideo: (TextureView) -> Unit = {},
    onDetachVideo: (TextureView) -> Unit = {},
) {
    val tokens = messengerTokens
    val copyEnabled = featureEnabled(FeatureFlag.MESSAGE_COPY)
    val replyEnabled = featureEnabled(FeatureFlag.MESSAGE_REPLY)
    val deleteForMeEnabled = featureEnabled(FeatureFlag.MESSAGE_DELETE_FOR_ME)
    val deleteForEveryoneEnabled = featureEnabled(FeatureFlag.MESSAGE_DELETE_FOR_EVERYONE)
    val replyJumpEnabled = featureEnabled(FeatureFlag.MESSAGE_REPLY_JUMP)
    val viewerEnabled = featureEnabled(FeatureFlag.MEDIA_VIEWER)
    val jumpToReply: (String?) -> Unit = { id -> if (replyJumpEnabled) id?.let(onReplyClick) }
    fun MutableList<MessageAction>.addDeleteActions() {
        if (isMe && deleteForEveryoneEnabled) add(MessageAction("Delete for everyone", Icons.Default.Delete, color = Color.Red) { onDelete(msg, true) })
        if (deleteForMeEnabled) add(MessageAction("Delete for me", Icons.Default.Delete, color = Color.Red) { onDelete(msg, false) })
    }
    fun MutableList<MessageAction>.addReplyAction() {
        if (replyEnabled) add(MessageAction("Reply", Icons.AutoMirrored.Filled.Reply, color = tokens.textPrimary) { onReply(msg) })
    }
    when (msg) {
        is Message.Call -> {
            val noop: () -> Unit = {}
            val callActions = buildList<MessageAction> { addDeleteActions() }
            val callCard: @Composable (live: Boolean) -> Unit = { live ->
                val onCall = when {
                    !live -> noop
                    msg.video -> onVideoCallClick
                    else -> onCallClick
                }
                when (msg.callType) {
                    CallType.MISSED -> MissedCallCard(
                        onCall = onCall,
                        timestamp = msg.timestamp,
                        isMe = isMe,
                        title = if (msg.video) "Missed video call" else "Missed Call",
                        status = msg.status,
                        video = msg.video,
                    )
                    CallType.UNREACHED -> MissedCallCard(
                        onCall = onCall,
                        timestamp = msg.timestamp,
                        isMe = isMe,
                        title = if (msg.video) "Unreached video call" else "Unreached call",
                        status = msg.status,
                        video = msg.video,
                    )
                    CallType.ENDED -> EndedCallCard(
                        durationSeconds = msg.durationSeconds,
                        timestamp = msg.timestamp,
                        isMe = isMe,
                        video = msg.video,
                    )
                }
            }
            LongPressMessage(
                isMe = isMe,
                messageKey = msg.id,
                actions = callActions,
                state = ctxState,
                preview = { callCard(false) },
            ) { callCard(true) }
        }
        is Message.Media -> {
            val mediaActions = buildList<MessageAction> {
                addReplyAction()
                addDeleteActions()
            }
            SwipeToReply(onReply = { onReply(msg) }) {
                LongPressMessage(
                    isMe = isMe,
                    messageKey = msg.id,
                    actions = mediaActions,
                    state = ctxState,
                    preview = {
                        MediaAlbumGrid(
                            message = msg,
                            isMe = isMe,
                            transfers = uiState.transfers,
                            onOpen = {},
                            onDownload = {},
                            onCancelUpload = { _, _ -> },
                            onCancelDownload = {},
                        )
                    },
                ) {
                    MediaAlbumGrid(
                        message = msg,
                        isMe = isMe,
                        transfers = uiState.transfers,
                        onOpen = { item ->
                            if (msg.status == MessageStatus.FAILED) {
                                onRetryMedia(msg.id)
                            } else if (viewerEnabled) {
                                val idx = uiState.conversationMedia
                                    .indexOfFirst { it.id == item.id }
                                if (idx >= 0) onOpenMedia(idx)
                            }
                        },
                        onDownload = { onDownload(it) },
                        onCancelUpload = { messageId, itemId -> onCancelUpload(messageId, itemId) },
                        onCancelDownload = { onCancelDownload(it) },
                    )
                }
            }
        }
        is Message.Text -> {
            val chatMessage = ChatMessage(
                text = msg.text,
                isMe = isMe,
                status = msg.status,
                timestamp = msg.timestamp,
                replyToMessageId = msg.replyToMessageId,
                replyToText = msg.replyToText,
                replyToSenderLabel = replySenderLabel(msg.replyToMessageId, msg.replyToSenderId, uiState),
            )
            val textActions = buildList {
                if (copyEnabled) add(MessageAction("Copy", Icons.Default.ContentCopy, color = tokens.textPrimary) { onCopy(msg.text) })
                addReplyAction()
                addDeleteActions()
            }
            SwipeToReply(onReply = { onReply(msg) }) {
                LongPressMessage(
                    isMe = isMe,
                    messageKey = msg.id,
                    actions = textActions,
                    state = ctxState,
                    preview = {
                        ChatTextBubble(message = chatMessage, highlighted = false, onReplyClick = {})
                    },
                ) {
                    ChatTextBubble(
                        message = chatMessage,
                        highlighted = msg.id == uiState.highlightedMessageId,
                        onReplyClick = { jumpToReply(msg.replyToMessageId) },
                    )
                }
            }
        }
        is Message.Clip -> {
            val clipActions = buildList<MessageAction> {
                addReplyAction()
                addDeleteActions()
            }
            val quoteLabel = replySenderLabel(msg.replyToMessageId, msg.replyToSenderId, uiState)
            val bubble: @Composable (interactive: Boolean) -> Unit = { interactive ->
                when (val clip = msg.clip) {
                    is Clip.Voice -> VoiceBubble(
                        clip = clip,
                        messageId = msg.id,
                        isMe = isMe,
                        played = msg.played,
                        status = msg.status,
                        timestamp = msg.timestamp,
                        playback = clipPlayback,
                        onTogglePlay = { onPlayClip(msg.id) },
                        onSeek = onSeekClip,
                        transfer = uiState.transfers[msg.id],
                        onRetrySend = { onRetryClip(msg.id) },
                        onCancelSend = { onCancelClipSend(msg.id) },
                        replySenderLabel = quoteLabel,
                        replyText = msg.replyToText,
                        onReplyClick = { jumpToReply(msg.replyToMessageId) },
                        interactive = interactive,
                    )
                    is Clip.Video -> VideoNoteBubble(
                        clip = clip,
                        messageId = msg.id,
                        isMe = isMe,
                        played = msg.played,
                        status = msg.status,
                        timestamp = msg.timestamp,
                        playback = clipPlayback,
                        onTogglePlay = { onPlayClip(msg.id) },
                        onSeek = onSeekClip,
                        videoFrame = videoFrame,
                        onAttachVideo = onAttachVideo,
                        onDetachVideo = onDetachVideo,
                        transfer = uiState.transfers[msg.id],
                        onRetrySend = { onRetryClip(msg.id) },
                        onCancelSend = { onCancelClipSend(msg.id) },
                        replySenderLabel = quoteLabel,
                        replyText = msg.replyToText,
                        onReplyClick = { jumpToReply(msg.replyToMessageId) },
                        interactive = interactive,
                    )
                }
            }
            SwipeToReply(onReply = { onReply(msg) }) {
                LongPressMessage(
                    isMe = isMe,
                    messageKey = msg.id,
                    actions = clipActions,
                    state = ctxState,
                    preview = { bubble(false) },
                ) {
                    bubble(true)
                }
            }
        }
    }
}

@Composable
private fun replySenderLabel(replyToMessageId: String?, replyToSenderId: String?, uiState: ChatUiState): String? {
    if (replyToMessageId == null) return null
    return if (replyToSenderId == uiState.currentUserId) {
        stringResource(R.string.chat_reply_you)
    } else {
        uiState.partnerUsername.ifBlank { stringResource(R.string.chat_reply_unknown_sender) }
    }
}
