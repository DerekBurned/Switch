package com.example.messenger.presentation.screens.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.messenger.presentation.theme.MessengerShapes
import com.example.messenger.presentation.theme.messengerTokens

@Composable
internal fun CallControlsRow(
    state: CallUiState,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onEnd: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MessengerShapes.card)
            .background(messengerTokens.pillFill)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.isIncoming) {
            IncomingCallControls(
                isVideoCall = state.isVideoCall,
                onAccept = onAccept,
                onDecline = onDecline,
            )
        } else {
            ActiveCallControls(
                state = state,
                onEnd = onEnd,
                onToggleSpeaker = onToggleSpeaker,
                onToggleMute = onToggleMute,
                onToggleCamera = onToggleCamera,
                onSwitchCamera = onSwitchCamera,
            )
        }
    }
}
