package com.example.messenger.presentation.state

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.MediaTransfer
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.UserPresence
import com.example.messenger.presentation.base.UiState
import com.example.messenger.presentation.base.UiText

@Immutable
data class PendingAttachment(val uri: Uri, val kind: String)

@Immutable
data class ChatUiState(
    val isLoading: Boolean = false,
    val messages: List<Message> = emptyList(),
    val error: UiText? = null,
    val isSending: Boolean = false,
    val partnerPresence: UserPresence = UserPresence(),
    val partnerUsername: String = "",
    val partnerAvatarUrl: String? = null,
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
) : UiState
