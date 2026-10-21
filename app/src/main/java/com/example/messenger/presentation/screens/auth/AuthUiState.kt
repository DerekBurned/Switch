package com.example.messenger.presentation.screens.auth

import com.example.messenger.domain.model.SessionState
import com.example.messenger.domain.model.User
import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.base.mvi.UiState

data class AuthUiState(
    val isLoading: Boolean = false,
    val currentUser: User? = null,
    val error: UiText? = null,
    val session: SessionState = SessionState.SignedOut(),
    val codeSent: Boolean = false,
    val verificationId: String? = null,
) : UiState
