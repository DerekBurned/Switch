package com.example.messenger.presentation.state

import com.example.messenger.domain.model.User
import com.example.messenger.presentation.base.UiText

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: UiText? = null,
    val isEditing: Boolean = false,
    val photos: List<String> = emptyList(),
)
