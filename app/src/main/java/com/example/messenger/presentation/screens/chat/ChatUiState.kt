package com.example.messenger.presentation.screens.chat

import androidx.compose.runtime.Immutable
import com.example.messenger.data.media.ClipRecording
import com.example.messenger.data.media.VideoRecordState
import com.example.messenger.data.media.VoiceRecordState
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.MediaTransfer
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.presence.UserPresence
import com.example.messenger.domain.model.voicemessage.ClipKind
import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.base.mvi.UiState

@Immutable
data class ChatUiState(
    val isLoading: Boolean = false,
    val messages: List<Message> = emptyList(),
    val error: UiText? = null,
    val isSending: Boolean = false,
    val partnerPresence: UserPresence = UserPresence(),
    val partnerUsername: String = "",
    val partnerAvatarUrl: String? = null,
    val partnerDeleted: Boolean = false,
    val typingUsernames: List<String> = emptyList(),
    val isPartnerTyping: Boolean = false,
    val replyingTo: Message? = null,
    val forwardingMessage: Message? = null,
    val currentUserId: String = "",

    val partnerLastSeenDisplay: String = "",

    val isLoadingOlder: Boolean = false,

    val hasMoreOlder: Boolean = true,

    val firstUnreadMessageId: String? = null,
    val unreadAnchorResolved: Boolean = false,

    val highlightedMessageId: String? = null,

    val transfers: Map<String, MediaTransfer> = emptyMap(),

    val pendingAttachments: List<PendingAttachment> = emptyList(),

    val conversationMedia: List<MediaItem> = emptyList(),

    val recording: VoiceRecordState = VoiceRecordState.Idle,

    val videoRecording: VideoRecordState = VideoRecordState.Idle,

    val clipMode: ClipKind = ClipKind.VOICE,

    val clipDraft: ClipRecording? = null,

    val micDenied: Boolean = false,

    val micDeniedForVideo: Boolean = false,
) : UiState
