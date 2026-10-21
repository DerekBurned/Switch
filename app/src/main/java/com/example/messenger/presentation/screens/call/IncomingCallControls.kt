package com.example.messenger.presentation.screens.call

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import com.example.messenger.presentation.components.call.CallControlButton
import com.example.messenger.presentation.theme.messengerTokens

@Composable
internal fun RowScope.IncomingCallControls(
    isVideoCall: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val tokens = messengerTokens
    CallControlButton(Icons.Default.CallEnd, "Decline", tokens.danger, onClick = onDecline)
    CallControlButton(
        if (isVideoCall) Icons.Default.Videocam else Icons.Default.Call,
        "Accept",
        tokens.callAccept,
        onClick = onAccept,
    )
}
