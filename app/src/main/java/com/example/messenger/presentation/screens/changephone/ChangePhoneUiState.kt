package com.example.messenger.presentation.screens.changephone

import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.base.mvi.UiState
import com.example.messenger.presentation.components.auth.model.Countries
import com.example.messenger.presentation.components.auth.model.Country

data class ChangePhoneUiState(
    val step: ChangePhoneStep = ChangePhoneStep.CONFIRM_CURRENT,
    val currentPhone: String = "",
    val newCountry: Country = Countries.default,
    val newNationalNumber: String = "",
    val otp: String = "",
    val verificationId: String? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
) : UiState
