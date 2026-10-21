package com.example.messenger.data.local.obx.mappers

import com.example.messenger.data.local.obx.ObxConversation
import com.example.messenger.domain.model.Conversation

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
