package com.example.messenger.presentation.components.search

import com.example.messenger.domain.model.User
import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.base.mvi.UiState

data class SearchUsersUiState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val error: UiText? = null,
    val isCreatingConversation: Boolean = false,
    val recentSearched: List<User> = emptyList(),
    val recentOpened: List<User> = emptyList(),
) : UiState
