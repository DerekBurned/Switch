package com.example.messenger.presentation.screens.settings.deleteaccount

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.example.messenger.R
import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.theme.MessengerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeleteAccountScreenContentTest {

    @get:Rule
    val compose = createComposeRule()

    private var startDeletionClicks = 0
    private var verifyClicks = 0

    private fun render(state: DeleteAccountUiState) {
        compose.setContent {
            MessengerTheme {
                DeleteAccountScreenContent(
                    state = state,
                    onStartDeletion = { startDeletionClicks++ },
                    onVerify = { verifyClicks++ },
                )
            }
        }
    }

    @Test
    fun `an error in state is shown`() {
        render(DeleteAccountUiState(error = UiText.StringResource(R.string.delete_account_error_confirm)))

        val message = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.delete_account_error_confirm)
        compose.onNodeWithText(message).assertExists()
    }

    @Test
    fun `the delete button stays disabled until DELETE is typed`() {
        render(DeleteAccountUiState(confirmation = "delete"))

        compose.onNodeWithText("Delete my account").assertIsNotEnabled()
    }

    @Test
    fun `the confirm step calls onStartDeletion`() {
        render(DeleteAccountUiState(confirmation = "DELETE"))

        compose.onNodeWithText("Delete my account").assertIsEnabled().performScrollTo().performClick()

        assertThat(startDeletionClicks).isEqualTo(1)
    }

    @Test
    fun `the verify step calls onVerify`() {
        render(DeleteAccountUiState(step = DeleteAccountStep.VERIFY, otp = "123456", phone = "+12025550123"))

        compose.onNodeWithText("Verify").performClick()

        assertThat(verifyClicks).isEqualTo(1)
    }
}
