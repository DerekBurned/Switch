package com.example.messenger.presentation.screens.call

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.messenger.domain.model.call.CallConnectionState
import com.example.messenger.domain.service.ICallService
import com.example.messenger.presentation.components.common.KeepScreenOn
import com.example.messenger.presentation.components.common.WallpaperBackground
import com.example.messenger.presentation.theme.MessengerTheme

@Composable
internal fun CallScreenContent(
    state: CallUiState,
    micDenied: Boolean = false,
    onBack: () -> Unit = {},
    onAccept: () -> Unit = {},
    onDecline: () -> Unit = {},
    onEnd: () -> Unit = {},
    onToggleSpeaker: () -> Unit = {},
    onToggleMute: () -> Unit = {},
    onToggleCamera: () -> Unit = {},
    onSwitchCamera: () -> Unit = {},
    callService: ICallService? = null,
) {
    val videoMode = !state.callEnded && (state.localVideoOn || state.remoteVideoOn)
    KeepScreenOn(enabled = !state.callEnded)
    if (videoMode) {
        VideoCallContent(
            state = state,
            micDenied = micDenied,
            onBack = onBack,
            onAccept = onAccept,
            onDecline = onDecline,
            onEnd = onEnd,
            onToggleSpeaker = onToggleSpeaker,
            onToggleMute = onToggleMute,
            onToggleCamera = onToggleCamera,
            onSwitchCamera = onSwitchCamera,
            callService = callService,
        )
    } else {
        AudioCallContent(
            state = state,
            micDenied = micDenied,
            onBack = onBack,
            onAccept = onAccept,
            onDecline = onDecline,
            onEnd = onEnd,
            onToggleSpeaker = onToggleSpeaker,
            onToggleMute = onToggleMute,
            onToggleCamera = onToggleCamera,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CallScreenIncomingPreview() {
    MessengerTheme {
        WallpaperBackground {
            CallScreenContent(
                state = CallUiState(
                    partnerName = "Alice",
                    partnerPhone = "+1 555 0100",
                    isIncoming = true,
                ),
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CallScreenIncomingVideoPreview() {
    MessengerTheme {
        WallpaperBackground {
            CallScreenContent(
                state = CallUiState(
                    partnerName = "Alice",
                    partnerPhone = "+1 555 0100",
                    isIncoming = true,
                    isVideoCall = true,
                ),
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CallScreenActivePreview() {
    MessengerTheme {
        WallpaperBackground {
            CallScreenContent(
                state = CallUiState(
                    partnerName = "Alice",
                    partnerPhone = "+1 555 0100",
                    isActive = true,
                    seconds = 73,
                    connectionState = CallConnectionState.CONNECTED,
                ),
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CallScreenRingingPreview() {
    MessengerTheme {
        WallpaperBackground {
            CallScreenContent(
                state = CallUiState(
                    partnerName = "Alice",
                    partnerPhone = "+1 555 0100",
                    remoteRinging = true,
                    connectionState = CallConnectionState.CONNECTED,
                ),
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CallScreenRequestingPreview() {
    MessengerTheme {
        WallpaperBackground {
            CallScreenContent(
                state = CallUiState(
                    partnerName = "Alice",
                    partnerPhone = "+1 555 0100",
                    connectionState = CallConnectionState.CONNECTED,
                ),
            )
        }
    }
}
