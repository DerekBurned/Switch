package com.example.messenger.presentation.state

import com.example.messenger.domain.model.Conversation
import com.example.messenger.presentation.base.UiState
import com.example.messenger.presentation.base.UiText

data class EditChatUiState(
    val isLoading: Boolean = false,
    val conversations: List<Conversation> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val error: UiText? = null,
) : UiState
