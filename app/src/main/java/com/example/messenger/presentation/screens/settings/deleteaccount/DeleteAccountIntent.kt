package com.example.messenger.presentation.screens.settings.deleteaccount

import android.app.Activity
import com.example.messenger.presentation.base.mvi.UiIntent

sealed interface DeleteAccountIntent : UiIntent {

    data class ConfirmationChange(val value: String) : DeleteAccountIntent

    data class OtpChange(val value: String) : DeleteAccountIntent

    data class SendVerificationCode(val activity: Activity) : DeleteAccountIntent

    data object VerifyOtpAndDelete : DeleteAccountIntent

    data object ClearError : DeleteAccountIntent

    data object GoBack : DeleteAccountIntent
}
