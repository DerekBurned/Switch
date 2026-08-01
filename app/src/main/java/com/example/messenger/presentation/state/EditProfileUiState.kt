package com.example.messenger.presentation.state

import com.example.messenger.presentation.base.UiState
import com.example.messenger.presentation.base.UiText

data class EditProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val name: String = "",
    val phone: String = "",
    val username: String = "",
    val dob: String = "",
    val avatarUrl: String? = null,
    val error: UiText? = null,
) : UiState
