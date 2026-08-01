package com.example.messenger.data.remote.dto

data class ConversationDTO(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: List<String> = emptyList(),
    val participantAvatars: List<String?> = emptyList(),
    val lastMessage: String? = null,
    val lastMessageTimestamp: Long = 0L,
    val unreadCount: Int = 0,
    val clearedAt: Map<String, Long> = emptyMap(),
    val visibleTo: List<String> = emptyList(),
)