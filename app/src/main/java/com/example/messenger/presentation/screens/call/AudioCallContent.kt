package com.example.messenger.presentation.screens.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.presentation.components.common.springEnter
import com.example.messenger.presentation.theme.messengerTokens

@Composable
internal fun AudioCallContent(
    state: CallUiState,
    micDenied: Boolean,
    onBack: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onEnd: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
) {
    val tokens = messengerTokens
    Box(modifier = Modifier.fillMaxSize()) {
        CallBackButton(
            onBack = onBack,
            tint = tokens.textPrimary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 10.dp, top = 12.dp),
        )
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.springEnter(initialScale = 0.88f),
            ) {
                CallAvatar(name = state.partnerName, photoUrl = state.partnerAvatarUrl)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = state.partnerName.ifBlank { "Calling…" },
                    color = tokens.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (state.partnerPhone.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(state.partnerPhone, color = tokens.textPrimary.copy(0.7f), fontSize = 16.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = callStatusText(state),
                    color = if (state.isActive && state.remotePresent) tokens.callAccept
                    else tokens.textPrimary.copy(0.7f),
                    fontSize = 16.sp,
                )
                if (micDenied) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Microphone permission is required to talk.",
                        color = tokens.danger,
                        fontSize = 13.sp,
                    )
                }
            }

            CallControlsRow(
                state = state,
                onAccept = onAccept,
                onDecline = onDecline,
                onEnd = onEnd,
                onToggleSpeaker = onToggleSpeaker,
                onToggleMute = onToggleMute,
                onToggleCamera = onToggleCamera,
                onSwitchCamera = {},
            )
        }
    }
}
