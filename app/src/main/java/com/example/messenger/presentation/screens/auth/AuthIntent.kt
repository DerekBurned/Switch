package com.example.messenger.presentation.screens.auth

import android.app.Activity
import com.example.messenger.presentation.base.mvi.UiIntent
import com.example.messenger.presentation.components.auth.model.AuthMode
import com.example.messenger.presentation.components.auth.model.Country

sealed interface AuthIntent : UiIntent {
    data class SelectMode(val mode: AuthMode) : AuthIntent
    data class SelectCountry(val country: Country) : AuthIntent
    data class ChangeNationalNumber(val input: String) : AuthIntent
    data class ChangeUsername(val username: String) : AuthIntent
    data class ChangeOtp(val otp: String) : AuthIntent

    data class SendVerificationCode(
        val activity: Activity,
        val phoneNumber: String,
        val username: String?,
        val isRegister: Boolean,
    ) : AuthIntent

    data class ResendCode(
        val activity: Activity,
        val phoneNumber: String,
    ) : AuthIntent

    data class VerifyOtpAndLogin(val otpCode: String) : AuthIntent
    data class VerifyOtpAndLink(val otpCode: String) : AuthIntent
    data object Logout : AuthIntent
    data object ClearError : AuthIntent
    data object EditPhoneNumber : AuthIntent
}
