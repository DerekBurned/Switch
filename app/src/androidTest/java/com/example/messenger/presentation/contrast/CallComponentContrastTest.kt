package com.example.messenger.presentation.contrast

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.messenger.data.remote.call.ActiveCallHolder
import com.example.messenger.presentation.components.call.CallControlButton
import com.example.messenger.presentation.components.call.IncomingCallBar
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallComponentContrastTest {

    @get:Rule
    val compose = createComposeRule()

    @After
    fun clearCall() = ActiveCallHolder.clear()

    private fun seedIncomingCall() = ActiveCallHolder.set(
        ActiveCallHolder.ActiveCall(
            callId = "call1",
            callerId = "u2",
            calleeId = Fixtures.CURRENT_USER_ID,
            channelName = "channel1",
            partnerName = "Bob Martin",
            partnerPhone = "+1 555 0200",
            isIncoming = true,
        ),
    )

    @Test
    fun callControlButtonNeutral() = compose.assertReadableInBothThemes(
        component = "CallControlButton",
        backdrop = Backdrop.VideoBlack,
    ) {
        CallControlButton(
            icon = Icons.Default.MicOff,
            label = "Mute",
            backgroundColor = Color.White.copy(alpha = 0.16f),
            onClick = {},
        )
    }

    @Test
    fun incomingCallBar() {
        seedIncomingCall()
        compose.assertReadableInBothThemes("IncomingCallBar") {
            IncomingCallBar(onAccept = {}, onDecline = {}, onOpen = {})
        }
    }
}
