package com.example.messenger.presentation.contrast

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.messenger.presentation.components.auth.AuthInputTextField
import com.example.messenger.presentation.components.auth.AuthModeToggle
import com.example.messenger.presentation.components.auth.ConfirmCurrentStep
import com.example.messenger.presentation.components.auth.CountryCodePicker
import com.example.messenger.presentation.components.auth.StepIndicator
import com.example.messenger.presentation.components.auth.model.AuthMode
import com.example.messenger.presentation.components.auth.model.Country
import com.example.messenger.presentation.screens.changephone.ChangePhoneStep
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthComponentContrastTest {

    private val country = Country(
        flagEmoji = "🇺🇸",
        name = "United States",
        isoCode = "US",
        dialCode = "+1",
    )

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun authInputTextFieldEmpty() = compose.assertReadableInBothThemes("AuthInputTextField empty") {
        AuthInputTextField(value = "", onValueChange = {}, placeholder = "Phone number")
    }

    @Test
    fun authInputTextFieldFilled() = compose.assertReadableInBothThemes("AuthInputTextField filled") {
        AuthInputTextField(value = "+1 555 0100", onValueChange = {}, placeholder = "Phone number")
    }

    @Test
    fun authModeToggle() = compose.assertReadableInBothThemes("AuthModeToggle") {
        AuthModeToggle(mode = AuthMode.LOGIN, onModeChange = {})
    }

    @Test
    fun countryCodePicker() = compose.assertReadableInBothThemes("CountryCodePicker") {
        CountryCodePicker(selected = country, onCountrySelected = {})
    }

    @Test
    fun stepIndicator() = compose.assertReadableInBothThemes("StepIndicator") {
        StepIndicator(step = ChangePhoneStep.ENTER_NEW)
    }

    @Test
    fun confirmCurrentStep() = compose.assertReadableInBothThemes("ConfirmCurrentStep") {
        ConfirmCurrentStep(currentPhone = "+1 555 0100", isLoading = false, onSendCode = {})
    }
}
