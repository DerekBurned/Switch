package com.example.messenger.presentation.screens.auth

import com.example.messenger.domain.model.SessionState
import com.example.messenger.domain.model.User
import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.base.mvi.UiState
import com.example.messenger.presentation.components.auth.model.AuthMode
import com.example.messenger.presentation.components.auth.model.Countries
import com.example.messenger.presentation.components.auth.model.Country

data class AuthUiState(
    val isLoading: Boolean = false,
    val currentUser: User? = null,
    val error: UiText? = null,
    val session: SessionState = SessionState.SignedOut(),
    val codeSent: Boolean = false,
    val verificationId: String? = null,
    val mode: AuthMode = AuthMode.REGISTER,
    val country: Country = Countries.default,
    val nationalNumber: String = "",
    val phoneValid: Boolean = false,
    val username: String = "",
    val otp: String = "",
) : UiState
