package com.example.messenger.data.remote.dto

import com.example.messenger.domain.model.CallType
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus

data class RemoteMessageDto(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val timestamp: Long = 0L,
    val status: String = "SENT",
    val isRead: Boolean = false,
    val deleted: Boolean = false,
    val deletedFor: List<String> = emptyList(),
    val type: String = "TEXT",
    val text: String = "",
    val replyToMessageId: String? = null,
    val replyToText: String? = null,
    val replyToSenderId: String? = null,
    val mediaItems: List<RemoteMediaItemDto> = emptyList(),
    val callDurationSeconds: Int = 0,
    val callVideo: Boolean = false,
    val enc: Int = 0,
    val ciphertext: String = "",
    val nonce: String = "",
    val senderEpoch: Int = 0,
    val recipientEpoch: Int = 0,
    val recipientId: String = "",
)

data class RemoteMediaItemDto(
    val id: String = "",
    val kind: String = "IMAGE",
    val url: String = "",
    val storagePath: String = "",
    val blurHash: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
)

fun RemoteMediaItemDto.toMediaItem(): MediaItem = MediaItem(
    id = id,
    kind = kind,
    url = url,
    storagePath = storagePath,
    blurHash = blurHash,
    width = width,
    height = height,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
)

fun MediaItem.toRemoteDto(): RemoteMediaItemDto = RemoteMediaItemDto(
    id = id,
    kind = kind,
    url = url,
    storagePath = storagePath,
    blurHash = blurHash,
    width = width,
    height = height,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
)

private fun parseStatus(value: String): MessageStatus {
    if (value == "DELIVERED" || value == MessageStatus.SENDING.name) return MessageStatus.SENT
    return runCatching { MessageStatus.valueOf(value) }.getOrDefault(MessageStatus.SENT)
}

fun RemoteMessageDto.toDomain(hasPendingWrites: Boolean): Message {
    val base = toDomain()
    if (!hasPendingWrites) return base
    return when (base) {
        is Message.Text  -> base.copy(status = MessageStatus.SENDING)
        is Message.Media -> base.copy(status = MessageStatus.SENDING)
        is Message.Call  -> base.copy(status = MessageStatus.SENDING)
    }
}

fun RemoteMessageDto.toDomain(): Message {
    val parsedStatus = parseStatus(status)
    val read = isRead || parsedStatus == MessageStatus.READ
    return when (type.ifBlank { Message.TYPE_TEXT }) {
        Message.TYPE_MEDIA -> Message.Media(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            timestamp = timestamp,
            status = parsedStatus,
            isRead = read,
            deleted = deleted,
            items = mediaItems.map { it.toMediaItem() },
            caption = text,
            replyToMessageId = replyToMessageId,
            replyToText = replyToText,
            replyToSenderId = replyToSenderId,
        )
        Message.TYPE_MISSED_CALL -> Message.Call(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            timestamp = timestamp,
            status = parsedStatus,
            isRead = read,
            deleted = deleted,
            callType = CallType.MISSED,
            video = callVideo,
        )
        Message.TYPE_UNREACHED_CALL -> Message.Call(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            timestamp = timestamp,
            status = parsedStatus,
            isRead = read,
            deleted = deleted,
            callType = CallType.UNREACHED,
            video = callVideo,
        )
        Message.TYPE_ENDED_CALL -> Message.Call(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            timestamp = timestamp,
            status = parsedStatus,
            isRead = read,
            deleted = deleted,
            callType = CallType.ENDED,
            durationSeconds = callDurationSeconds,
            video = callVideo,
        )
        else -> Message.Text(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            timestamp = timestamp,
            status = parsedStatus,
            isRead = read,
            deleted = deleted,
            text = text,
            replyToMessageId = replyToMessageId,
            replyToText = replyToText,
            replyToSenderId = replyToSenderId,
        )
    }
}

fun Message.toRemoteDto(): RemoteMessageDto = when (this) {
    is Message.Text -> RemoteMessageDto(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        timestamp = timestamp,
        status = status.name,
        isRead = isRead,
        deleted = deleted,
        type = Message.TYPE_TEXT,
        text = text,
        replyToMessageId = replyToMessageId,
        replyToText = replyToText,
        replyToSenderId = replyToSenderId,
    )
    is Message.Media -> RemoteMessageDto(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        timestamp = timestamp,
        status = status.name,
        isRead = isRead,
        deleted = deleted,
        type = Message.TYPE_MEDIA,
        text = caption,
        replyToMessageId = replyToMessageId,
        replyToText = replyToText,
        replyToSenderId = replyToSenderId,
        mediaItems = items.map { it.toRemoteDto() },
    )
    is Message.Call -> RemoteMessageDto(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        timestamp = timestamp,
        status = status.name,
        isRead = isRead,
        deleted = deleted,
        type = when (callType) {
            CallType.MISSED    -> Message.TYPE_MISSED_CALL
            CallType.UNREACHED -> Message.TYPE_UNREACHED_CALL
            CallType.ENDED     -> Message.TYPE_ENDED_CALL
        },
        callDurationSeconds = durationSeconds,
        callVideo = video,
    )
}
