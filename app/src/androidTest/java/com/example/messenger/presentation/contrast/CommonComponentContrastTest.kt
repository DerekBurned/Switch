package com.example.messenger.presentation.contrast

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.messenger.data.media.ClipPlaybackState
import com.example.messenger.presentation.components.common.ClipMiniPlayer
import com.example.messenger.presentation.components.common.MessengerAvatar
import com.example.messenger.presentation.components.common.MessengerInputField
import com.example.messenger.presentation.components.common.MessengerSearchField
import com.example.messenger.presentation.components.common.NavHeaderPill
import com.example.messenger.presentation.components.common.PillButton
import com.example.messenger.presentation.components.common.SectionHeader
import com.example.messenger.presentation.components.common.SegmentedToggle
import com.example.messenger.presentation.components.common.SettingsRow
import com.example.messenger.presentation.components.common.model.PillButtonStyle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommonComponentContrastTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun messengerAvatar() = compose.assertReadableInBothThemes("MessengerAvatar") {
        Column {
            MessengerAvatar(name = "Alice")
            MessengerAvatar(name = "Bob")
            MessengerAvatar(name = "Carol")
            MessengerAvatar(name = "Dave")
            MessengerAvatar(name = "Erin")
            MessengerAvatar(name = "Frank")
            MessengerAvatar(name = "Grace")
        }
    }

    @Test
    fun messengerSearchField() = compose.assertReadableInBothThemes("MessengerSearchField") {
        MessengerSearchField(value = "", onValueChange = {})
    }

    @Test
    fun messengerSearchFieldFilled() = compose.assertReadableInBothThemes("MessengerSearchField filled") {
        MessengerSearchField(value = "Alice", onValueChange = {})
    }

    @Test
    fun messengerInputField() = compose.assertReadableInBothThemes("MessengerInputField") {
        MessengerInputField(value = "Typed value", onValueChange = {}, placeholder = "Placeholder")
    }

    @Test
    fun pillButtonPrimary() = compose.assertReadableInBothThemes("PillButton primary") {
        PillButton(text = "Continue", onClick = {}, style = PillButtonStyle.Primary)
    }

    @Test
    fun pillButtonNeutral() = compose.assertReadableInBothThemes("PillButton neutral") {
        PillButton(text = "Cancel", onClick = {}, style = PillButtonStyle.Neutral)
    }

    @Test
    fun segmentedToggle() = compose.assertReadableInBothThemes("SegmentedToggle") {
        SegmentedToggle(options = listOf("Chats", "Calls"), selectedIndex = 0, onSelect = {})
    }

    @Test
    fun navHeaderPill() = compose.assertReadableInBothThemes("NavHeaderPill") {
        NavHeaderPill(title = "Settings", onBack = {})
    }

    @Test
    fun sectionHeader() = compose.assertReadableInBothThemes("SectionHeader") {
        SectionHeader(text = "Account")
    }

    @Test
    fun settingsRow() = compose.assertReadableInBothThemes("SettingsRow", Backdrop.Card) {
        SettingsRow(title = "Privacy", subtitle = "Blocked contacts", onClick = {})
    }

    @Test
    fun clipMiniPlayer() = compose.assertReadableInBothThemes("ClipMiniPlayer") {
        ClipMiniPlayer(
            playback = remember {
                mutableStateOf(
                    ClipPlaybackState(
                        messageId = "m1",
                        conversationId = "c1",
                        positionMs = 4_000L,
                        durationMs = 12_000L,
                        isPlaying = true,
                    ),
                )
            },
            onToggle = {},
            onCycleSpeed = {},
            onClose = {},
            onOpen = {},
        )
    }
}
