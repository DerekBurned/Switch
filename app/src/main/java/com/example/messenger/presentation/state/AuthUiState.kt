package com.example.messenger.presentation.state

import com.example.messenger.domain.model.User
import com.example.messenger.presentation.base.UiState
import com.example.messenger.presentation.base.UiText

data class AuthUiState(
    val isLoading: Boolean = false,
    val currentUser: User? = null,
    val error: UiText? = null,
    val isAuthenticated: Boolean = false,
    val codeSent: Boolean = false,
) : UiState
