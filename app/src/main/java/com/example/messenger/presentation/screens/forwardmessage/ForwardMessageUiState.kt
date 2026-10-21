package com.example.messenger.presentation.screens.forwardmessage

import com.example.messenger.domain.model.Conversation
import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.base.mvi.UiState

data class ForwardMessageUiState(
    val isLoading: Boolean = false,
    val conversations: List<Conversation> = emptyList(),
    val query: String = "",
    val searchActive: Boolean = false,
    val error: UiText? = null,
) : UiState {
    val filteredConversations: List<Conversation>
        get() = if (query.isBlank()) conversations
        else conversations.filter { conv ->
            conv.participantNames.any { it.contains(query, ignoreCase = true) }
        }
}
