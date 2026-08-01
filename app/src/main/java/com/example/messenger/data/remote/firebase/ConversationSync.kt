package com.example.messenger.data.remote.firebase

import com.example.messenger.domain.model.Conversation

data class ConversationSync(
    val conversations: List<Conversation>,
    val removedIds: List<String>,
    val fromCache: Boolean,
)
