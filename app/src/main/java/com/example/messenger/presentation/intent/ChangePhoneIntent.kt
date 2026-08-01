package com.example.messenger.presentation.intent

import android.app.Activity
import com.example.messenger.presentation.base.UiIntent
import com.example.messenger.presentation.components.auth.Country

sealed interface ChangePhoneIntent : UiIntent {
    data class NewCountrySelected(val country: Country) : ChangePhoneIntent
    data class NewNumberChange(val value: String) : ChangePhoneIntent
    data class OtpChange(val value: String) : ChangePhoneIntent
    data object ClearError : ChangePhoneIntent
    data class SendCodeToCurrentPhone(val activity: Activity) : ChangePhoneIntent
    data object VerifyCurrentOtp : ChangePhoneIntent
    data class SendCodeToNewPhone(val activity: Activity) : ChangePhoneIntent
    data object VerifyNewOtp : ChangePhoneIntent
    data object GoBack : ChangePhoneIntent
}
