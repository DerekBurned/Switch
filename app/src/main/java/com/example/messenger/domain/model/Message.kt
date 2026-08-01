package com.example.messenger.domain.model

import androidx.compose.runtime.Immutable

@Immutable
sealed class Message {
    abstract val id: String
    abstract val conversationId: String
    abstract val senderId: String
    abstract val timestamp: Long
    abstract val status: MessageStatus
    abstract val isRead: Boolean
    abstract val deleted: Boolean

    data class Text(
        override val id: String = "",
        override val conversationId: String = "",
        override val senderId: String = "",
        override val timestamp: Long = 0L,
        override val status: MessageStatus = MessageStatus.SENT,
        override val isRead: Boolean = false,
        override val deleted: Boolean = false,
        val text: String = "",
        val replyToMessageId: String? = null,
        val replyToText: String? = null,
        val replyToSenderId: String? = null,
    ) : Message()

    data class Media(
        override val id: String = "",
        override val conversationId: String = "",
        override val senderId: String = "",
        override val timestamp: Long = 0L,
        override val status: MessageStatus = MessageStatus.SENT,
        override val isRead: Boolean = false,
        override val deleted: Boolean = false,
        val items: List<MediaItem> = emptyList(),
        val caption: String = "",
        val replyToMessageId: String? = null,
        val replyToText: String? = null,
        val replyToSenderId: String? = null,
    ) : Message()

    data class Call(
        override val id: String = "",
        override val conversationId: String = "",
        override val senderId: String = "",
        override val timestamp: Long = 0L,
        override val status: MessageStatus = MessageStatus.SENT,
        override val isRead: Boolean = false,
        override val deleted: Boolean = false,
        val callType: CallType = CallType.ENDED,
        val durationSeconds: Int = 0,
        val video: Boolean = false,
    ) : Message()

    companion object {
        const val TYPE_TEXT           = "TEXT"
        const val TYPE_MEDIA          = "MEDIA"
        const val TYPE_MISSED_CALL    = "MISSED_CALL"
        const val TYPE_UNREACHED_CALL = "UNREACHED_CALL"
        const val TYPE_ENDED_CALL     = "ENDED_CALL"
    }
}

enum class CallType { MISSED, UNREACHED, ENDED }
