package com.example.messenger.presentation.screens.editchat

import com.example.messenger.domain.model.Conversation
import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.base.mvi.UiState

data class EditChatUiState(
    val isLoading: Boolean = false,
    val conversations: List<Conversation> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val error: UiText? = null,
) : UiState
