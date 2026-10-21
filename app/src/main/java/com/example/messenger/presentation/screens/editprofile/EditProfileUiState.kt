package com.example.messenger.presentation.screens.editprofile

import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.base.mvi.UiState

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
