package com.example.messenger.presentation.components.chat

import androidx.compose.runtime.Immutable
import com.example.messenger.domain.model.MessageStatus

@Immutable
data class ChatMessage(
    val text: String,
    val isMe: Boolean,
    val status: MessageStatus = MessageStatus.SENT,
    val timestamp: Long = 0L,
    val replyToMessageId: String? = null,
    val replyToText: String? = null,
    val replyToSenderLabel: String? = null,
)
