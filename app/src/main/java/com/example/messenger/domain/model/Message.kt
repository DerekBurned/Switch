package com.example.messenger.domain.model

import androidx.compose.runtime.Immutable
import com.example.messenger.domain.model.call.CallType
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

    data class Clip(
        override val id: String = "",
        override val conversationId: String = "",
        override val senderId: String = "",
        override val timestamp: Long = 0L,
        override val status: MessageStatus = MessageStatus.SENT,
        override val isRead: Boolean = false,
        override val deleted: Boolean = false,
        val clip: com.example.messenger.domain.model.voicemessage.Clip,
        val played: Boolean = false,
        val replyToMessageId: String? = null,
        val replyToText: String? = null,
        val replyToSenderId: String? = null,
    ) : Message()

    fun replySnippet(): String? = when (this) {
        is Text -> text.take(REPLY_SNIPPET_MAX)
        is Media -> caption.take(REPLY_SNIPPET_MAX).ifBlank { "Media" }
        is Clip -> clip.previewLabel
        is Call -> null
    }

    companion object {
        const val REPLY_SNIPPET_MAX = 200
        const val TYPE_TEXT           = "TEXT"
        const val TYPE_MEDIA          = "MEDIA"
        const val TYPE_MISSED_CALL    = "MISSED_CALL"
        const val TYPE_UNREACHED_CALL = "UNREACHED_CALL"
        const val TYPE_ENDED_CALL     = "ENDED_CALL"
        const val TYPE_VOICE_CLIP     = "VOICE_CLIP"
        const val TYPE_VIDEO_CLIP     = "VIDEO_CLIP"
    }
}
