package com.example.messenger.presentation.state

import com.example.messenger.presentation.base.UiState
import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.components.auth.Countries
import com.example.messenger.presentation.components.auth.Country

enum class ChangePhoneStep {
    
    CONFIRM_CURRENT,

    VERIFY_CURRENT,

    ENTER_NEW,

    VERIFY_NEW,
}

data class ChangePhoneUiState(
    val step: ChangePhoneStep = ChangePhoneStep.CONFIRM_CURRENT,
    val currentPhone: String = "",
    val newCountry: Country = Countries.default,
    val newNationalNumber: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null,
) : UiState
