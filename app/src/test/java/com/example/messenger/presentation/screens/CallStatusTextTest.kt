package com.example.messenger.presentation.screens

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.example.messenger.domain.service.CallConnectionState
import com.example.messenger.presentation.state.CallUiState
import org.junit.jupiter.api.Test

class CallStatusTextTest {

    @Test
    fun `incoming call shows incoming regardless of other flags`() {
        val state = CallUiState(
            isIncoming = true,
            remoteRinging = true,
            connectionState = CallConnectionState.CONNECTED,
        )
        assertThat(callStatusText(state)).isEqualTo("Incoming call…")
    }

    @Test
    fun `incoming video call shows the video label`() {
        val state = CallUiState(
            isIncoming = true,
            isVideoCall = true,
            connectionState = CallConnectionState.CONNECTED,
        )
        assertThat(callStatusText(state)).isEqualTo("Incoming video call…")
    }

    @Test
    fun `active video call shows the elapsed timer like audio`() {
        val state = CallUiState(
            isActive = true,
            isVideoCall = true,
            remotePresent = true,
            seconds = 5,
            connectionState = CallConnectionState.CONNECTED,
        )
        assertThat(callStatusText(state)).isEqualTo("0:05")
    }

    @Test
    fun `connected but no ringing ack is still Requesting`() {
        val state = CallUiState(connectionState = CallConnectionState.CONNECTED)
        assertThat(callStatusText(state)).isEqualTo("Requesting…")
    }

    @Test
    fun `still connecting socket with no ack is Requesting`() {
        val state = CallUiState(connectionState = CallConnectionState.CONNECTING)
        assertThat(callStatusText(state)).isEqualTo("Requesting…")
    }

    @Test
    fun `ringing ack flips the label to Ringing`() {
        val state = CallUiState(
            remoteRinging = true,
            connectionState = CallConnectionState.CONNECTED,
        )
        assertThat(callStatusText(state)).isEqualTo("Ringing…")
    }

    @Test
    fun `accepted but media not yet flowing is Connecting`() {
        val state = CallUiState(
            isActive = true,
            remoteRinging = true,
            connectionState = CallConnectionState.CONNECTING,
        )
        assertThat(callStatusText(state)).isEqualTo("Connecting…")
    }

    @Test
    fun `active and connected with remote present shows the elapsed timer`() {
        val state = CallUiState(
            isActive = true,
            remotePresent = true,
            seconds = 73,
            connectionState = CallConnectionState.CONNECTED,
        )
        assertThat(callStatusText(state)).isEqualTo("1:13")
    }

    @Test
    fun `active and connected but remote not present is Connecting`() {
        val state = CallUiState(
            isActive = true,
            remotePresent = false,
            seconds = 73,
            connectionState = CallConnectionState.CONNECTED,
        )
        assertThat(callStatusText(state)).isEqualTo("Connecting…")
    }

    @Test
    fun `ended call shows Call Ended regardless of other flags`() {
        val state = CallUiState(
            callEnded = true,
            isActive = true,
            remotePresent = true,
            connectionState = CallConnectionState.CONNECTED,
        )
        assertThat(callStatusText(state)).isEqualTo("Call Ended")
    }

    @Test
    fun `failed connection shows call failed`() {
        val state = CallUiState(connectionState = CallConnectionState.FAILED)
        assertThat(callStatusText(state)).isEqualTo("Call failed")
    }

    @Test
    fun `disconnected shows disconnected`() {
        val state = CallUiState(connectionState = CallConnectionState.DISCONNECTED)
        assertThat(callStatusText(state)).isEqualTo("Disconnected")
    }
}
