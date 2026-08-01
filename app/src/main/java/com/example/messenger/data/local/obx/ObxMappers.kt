package com.example.messenger.data.local.obx

import com.example.messenger.domain.model.CallType
import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.PhoneVisibility
import com.example.messenger.domain.model.User

fun ObxMessage.toDomain(): Message {
    val parsedStatus = parseStatus(status)
    return when (type.ifBlank { Message.TYPE_TEXT }) {
        Message.TYPE_MEDIA -> Message.Media(
            id = uid,
            conversationId = conversationId,
            senderId = senderId,
            timestamp = timestamp,
            status = parsedStatus,
            isRead = isRead,
            deleted = deleted,
            items = mediaItemsJson,
            caption = text,
            replyToMessageId = replyToMessageId,
            replyToText = replyToText,
            replyToSenderId = replyToSenderId,
        )
        Message.TYPE_MISSED_CALL -> Message.Call(
            id = uid,
            conversationId = conversationId,
            senderId = senderId,
            timestamp = timestamp,
            status = parsedStatus,
            isRead = isRead,
            deleted = deleted,
            callType = CallType.MISSED,
            video = callVideo,
        )
        Message.TYPE_UNREACHED_CALL -> Message.Call(
            id = uid,
            conversationId = conversationId,
            senderId = senderId,
            timestamp = timestamp,
            status = parsedStatus,
            isRead = isRead,
            deleted = deleted,
            callType = CallType.UNREACHED,
            video = callVideo,
        )
        Message.TYPE_ENDED_CALL -> Message.Call(
            id = uid,
            conversationId = conversationId,
            senderId = senderId,
            timestamp = timestamp,
            status = parsedStatus,
            isRead = isRead,
            deleted = deleted,
            callType = CallType.ENDED,
            durationSeconds = callDurationSeconds,
            video = callVideo,
        )
        else -> Message.Text(
            id = uid,
            conversationId = conversationId,
            senderId = senderId,
            timestamp = timestamp,
            status = parsedStatus,
            isRead = isRead,
            deleted = deleted,
            text = text,
            replyToMessageId = replyToMessageId,
            replyToText = replyToText,
            replyToSenderId = replyToSenderId,
        )
    }
}

fun Message.toObx(): ObxMessage = when (this) {
    is Message.Text -> ObxMessage(
        uid = id,
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
        callDurationSeconds = 0,
        mediaItemsJson = emptyList(),
    )
    is Message.Media -> ObxMessage(
        uid = id,
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
        callDurationSeconds = 0,
        mediaItemsJson = items,
    )
    is Message.Call -> ObxMessage(
        uid = id,
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
        text = "",
        callDurationSeconds = durationSeconds,
        callVideo = video,
        mediaItemsJson = emptyList(),
    )
}

private fun parseStatus(value: String): MessageStatus {
    if (value == "DELIVERED") return MessageStatus.SENT
    return runCatching { MessageStatus.valueOf(value) }.getOrDefault(MessageStatus.SENT)
}

fun ObxUser.toDomain(): User = User(
    id = uid,
    username = username,
    usernameLower = usernameLower.ifBlank { username.lowercase() },
    email = email,
    phoneNumber = phoneNumber,
    avatarUrl = avatarUrl,
    lastSeen = lastSeen,
    isOnline = isOnline,
    phoneVisibility = PhoneVisibility.HIDDEN,
    fcmToken = null,
)

fun User.toObx(): ObxUser = ObxUser(
    uid = id,
    username = username ?: "Unknown",
    usernameLower = (usernameLower ?: username)?.lowercase().orEmpty(),
    email = email,
    phoneNumber = phoneNumber,
    avatarUrl = avatarUrl,
    lastSeen = lastSeen,
    isOnline = isOnline,
)

fun ObxConversation.toDomain(): Conversation {
    val useLatest = latestMessageText != null && latestMessageTimestamp >= lastMessageTimestamp
    return Conversation(
        id = uid,
        participantIds = participantIds,
        participantNames = participantNames,
        participantAvatars = participantAvatars,
        lastMessage = if (useLatest) latestMessageText else lastMessage,
        lastMessageSenderId = if (useLatest) latestMessageSenderId else lastMessageSenderId,
        lastMessageTimestamp = if (useLatest) latestMessageTimestamp else lastMessageTimestamp,
        unreadCount = unreadCount,
    )
}

fun Conversation.toObx(): ObxConversation = ObxConversation(
    uid = id,
    participantIds = participantIds,
    participantNames = participantNames,
    participantAvatars = participantAvatars,
    lastMessage = lastMessage,
    lastMessageSenderId = lastMessageSenderId,
    lastMessageTimestamp = lastMessageTimestamp,
    unreadCount = unreadCount,
)
