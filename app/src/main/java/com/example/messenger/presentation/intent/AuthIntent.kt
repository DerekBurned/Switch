package com.example.messenger.presentation.intent

import android.app.Activity
import com.example.messenger.presentation.base.UiIntent

sealed interface AuthIntent : UiIntent {
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
