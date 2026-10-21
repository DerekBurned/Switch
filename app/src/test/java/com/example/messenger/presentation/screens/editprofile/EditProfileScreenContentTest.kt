package com.example.messenger.presentation.screens.editprofile

import android.content.Context
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
class EditProfileScreenContentTest {

    @get:Rule
    val compose = createComposeRule()

    private var saveClicks = 0

    private fun render(state: EditProfileUiState) {
        compose.setContent {
            MessengerTheme {
                EditProfileScreenContent(state = state, onSaveClick = { saveClicks++ })
            }
        }
    }

    @Test
    fun `the blank-username error is shown`() {
        render(EditProfileUiState(error = UiText.StringResource(R.string.edit_profile_error_username_empty)))

        val message = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.edit_profile_error_username_empty)
        compose.onNodeWithText(message).assertExists()
    }

    @Test
    fun `Confirm changes calls onSaveClick`() {
        render(EditProfileUiState(username = "bob"))

        compose.onNodeWithText("Confirm changes").performScrollTo().performClick()

        assertThat(saveClicks).isEqualTo(1)
    }
}
