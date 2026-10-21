package com.example.messenger.presentation.screens.auth

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.example.messenger.presentation.components.auth.model.AuthMode
import com.example.messenger.presentation.components.auth.model.Countries
import com.example.messenger.presentation.theme.MessengerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthScreenContentTest {

    @get:Rule
    val compose = createComposeRule()

    private var sendClicks = 0
    private var verifyClicks = 0

    private fun render(
        mode: AuthMode = AuthMode.LOGIN,
        nationalNumber: String = "2025550123",
        username: String = "",
        otp: String = "",
        error: String? = null,
        codeSent: Boolean = false,
    ) {
        compose.setContent {
            MessengerTheme {
                AuthScreenContent(
                    mode = mode,
                    selectedCountry = Countries.all.first { it.isoCode == "US" },
                    nationalNumber = nationalNumber,
                    username = username,
                    otp = otp,
                    isLoading = false,
                    error = error,
                    codeSent = codeSent,
                    onModeChange = {},
                    onCountryChange = {},
                    onNationalNumberChange = {},
                    onUsernameChange = {},
                    onOtpChange = {},
                    onSendOtp = { sendClicks++ },
                    onVerifyOtp = { verifyClicks++ },
                )
            }
        }
    }

    @Test
    fun `before the code is sent, Log in calls onSendOtp`() {
        render()

        compose.onNodeWithText("Log in").performClick()

        assertThat(sendClicks).isEqualTo(1)
        assertThat(verifyClicks).isEqualTo(0)
    }

    @Test
    fun `after the code is sent, Verify code calls onVerifyOtp`() {
        render(otp = "123456", codeSent = true)

        compose.onNodeWithText("Verify code").performClick()

        assertThat(verifyClicks).isEqualTo(1)
        assertThat(sendClicks).isEqualTo(0)
    }

    @Test
    fun `an error is shown`() {
        render(error = "Too many attempts")

        compose.onNodeWithText("Too many attempts").assertExists()
    }

    @Test
    fun `an empty phone number disables Log in`() {
        render(nationalNumber = "")

        compose.onNodeWithText("Log in").assertIsNotEnabled()
    }

    @Test
    fun `registering without a username disables Sign in`() {
        render(mode = AuthMode.REGISTER, username = "")

        compose.onNodeWithText("Sign in").assertIsNotEnabled()
    }
}
