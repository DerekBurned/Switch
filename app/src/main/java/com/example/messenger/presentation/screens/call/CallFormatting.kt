package com.example.messenger.presentation.screens.call

import androidx.compose.material.icons.filled.Call
import com.example.messenger.domain.model.call.CallConnectionState

internal fun callStatusText(state: CallUiState): String = when {
    state.callEnded -> "Call Ended"
    state.isIncoming && state.isVideoCall -> "Incoming video call…"
    state.isIncoming -> "Incoming call…"
    state.isActive && state.connectionState == CallConnectionState.CONNECTED && state.remotePresent ->
        formatSeconds(state.seconds)
    state.isActive -> "Connecting…"
    state.connectionState == CallConnectionState.FAILED -> "Call failed"
    state.connectionState == CallConnectionState.DISCONNECTED -> "Disconnected"
    state.remoteRinging -> "Ringing…"
    else -> "Requesting…"
}


private fun formatSeconds(s: Int): String {
    val m = s / 60
    val sec = s % 60
    return "$m:${sec.toString().padStart(2, '0')}"
}
