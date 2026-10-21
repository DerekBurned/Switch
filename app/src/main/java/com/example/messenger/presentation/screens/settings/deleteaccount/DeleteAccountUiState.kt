package com.example.messenger.presentation.screens.settings.deleteaccount

import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.base.mvi.UiState

data class DeleteAccountUiState(
    val step: DeleteAccountStep = DeleteAccountStep.CONFIRM,
    val confirmation: String = "",
    val phone: String = "",
    val otp: String = "",
    val verificationId: String? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
) : UiState {

    val isConfirmed: Boolean get() = confirmation.trim() == CONFIRMATION_WORD

    companion object {
        const val CONFIRMATION_WORD = "DELETE"
    }
}
