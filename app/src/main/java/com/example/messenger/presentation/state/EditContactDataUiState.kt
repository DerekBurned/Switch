package com.example.messenger.presentation.state

import com.example.messenger.presentation.base.UiState
import com.example.messenger.presentation.base.UiText

data class EditContactDataUiState(
    val contactId: String = "",
    val username: String = "",
    val name: String = "",
    val initialName: String = "",
    val isSaving: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val error: UiText? = null,
) : UiState
