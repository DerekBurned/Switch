package com.example.messenger.presentation.screens.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.example.messenger.domain.model.User
import com.example.messenger.presentation.theme.MessengerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreenContentTest {

    @get:Rule
    val compose = createComposeRule()

    private val openedUris = mutableListOf<String>()
    private var hasBrowser = true

    private fun render() {
        compose.setContent {
            CompositionLocalProvider(
                LocalUriHandler provides object : UriHandler {
                    override fun openUri(uri: String) {
                        require(hasBrowser) { "Can't open $uri." }
                        openedUris += uri
                    }
                },
            ) {
                MessengerTheme {
                    SettingsScreenContent(user = User(id = "uid", username = "Jane"))
                }
            }
        }
    }

    @Test
    fun `the footer shows the copyright, the license and the missing warranty`() {
        render()

        compose.onNodeWithText("© 2025-2026 Derek Burned and contributors").performScrollTo().assertExists()
        compose.onNodeWithText(
            "Free software: you may share and modify it under the GNU AGPL version 3. " +
                "It comes with no warranty. Tap to view the license.",
        ).assertExists()
        assertThat(openedUris).isEmpty()
    }

    @Test
    fun `tapping the notice opens the AGPL text`() {
        render()

        compose.onNodeWithText("© 2025-2026 Derek Burned and contributors").performScrollTo().performClick()

        assertThat(openedUris).containsExactly("https://www.gnu.org/licenses/agpl-3.0.html")
    }

    @Test
    fun `tapping the notice without a browser does not crash`() {
        hasBrowser = false
        render()

        compose.onNodeWithText("© 2025-2026 Derek Burned and contributors").performScrollTo().performClick()

        assertThat(openedUris).isEmpty()
    }
}
