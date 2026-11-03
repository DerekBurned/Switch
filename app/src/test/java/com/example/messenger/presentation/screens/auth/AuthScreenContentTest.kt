package com.example.messenger.presentation.screens.auth

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
    private var changeNumberClicks = 0
    private val phoneField =
        SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText) and hasText("2025550123")

    private fun render(
        mode: AuthMode = AuthMode.LOGIN,
        nationalNumber: String = "2025550123",
        username: String = "",
        otp: String = "",
        error: String? = null,
        codeSent: Boolean = false,
        phoneValid: Boolean = nationalNumber.isNotEmpty(),
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
                    phoneValid = phoneValid,
                    onChangeNumber = { changeNumberClicks++ },
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
    fun `an invalid phone number disables Log in`() {
        render(nationalNumber = "202555", phoneValid = false)

        compose.onNodeWithText("Log in").assertIsNotEnabled()
    }

    @Test
    fun `the phone field is editable before the code is sent`() {
        render()

        compose.onNode(phoneField).assertIsEnabled()
    }

    @Test
    fun `tapping the phone field after the username field focuses the phone field`() {
        render(mode = AuthMode.REGISTER, username = "tester")

        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText) and hasText("tester"))
            .performClick()
        compose.onNode(phoneField).performClick()

        compose.onNode(phoneField).assertIsFocused()
    }

    @Test
    fun `after the code is sent, the phone field is locked`() {
        render(codeSent = true)

        compose.onNode(phoneField).assertIsNotEnabled()
    }

    @Test
    fun `the country picker is tappable before the code is sent`() {
        render()

        compose.onNodeWithContentDescription("Choose country").assertIsEnabled()
    }

    @Test
    fun `after the code is sent, the country picker is locked`() {
        render(codeSent = true)

        compose.onNodeWithContentDescription("Choose country").assertIsNotEnabled()
    }

    @Test
    fun `after the code is sent, Change number calls onChangeNumber`() {
        render(codeSent = true)

        compose.onNodeWithText("Change number").performClick()

        assertThat(changeNumberClicks).isEqualTo(1)
    }

    @Test
    fun `registering without a username disables Sign in`() {
        render(mode = AuthMode.REGISTER, username = "")

        compose.onNodeWithText("Sign in").assertIsNotEnabled()
    }
}
