package com.example.messenger.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class  Conversation(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: List<String> = emptyList(),
    val participantAvatars: List<String?> = emptyList(),
    val lastMessage: String? = null,
    val lastMessageSenderId: String = "",
    val lastMessageTimestamp: Long = 0L,
    val unreadCount: Int = 0,
    val unreadCounts: Map<String, Int> = emptyMap(),
    val clearedAt: Map<String, Long> = emptyMap(),
    val visibleTo: List<String> = emptyList(),
)