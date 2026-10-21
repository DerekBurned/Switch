package com.example.messenger.presentation.screens.call

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.presentation.components.call.CallControlButton
import com.example.messenger.presentation.featureflag.featureEnabled
import com.example.messenger.presentation.theme.messengerTokens

@Composable
internal fun RowScope.ActiveCallControls(
    state: CallUiState,
    onEnd: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
) {
    val tokens = messengerTokens
    if (featureEnabled(FeatureFlag.CALLS_SPEAKER)) {
        CallControlButton(
            Icons.Default.VolumeUp,
            "Speaker",
            if (state.speakerOn) tokens.accent else tokens.neutralButtonFill,
            iconTint = if (state.speakerOn) tokens.onAccent else tokens.textPrimary,
            size = 52.dp,
            onClick = onToggleSpeaker,
        )
    }
    if (state.localVideoOn || featureEnabled(FeatureFlag.CALLS_VIDEO)) {
        CallControlButton(
            if (state.localVideoOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
            "Video",
            if (state.localVideoOn) tokens.accent else tokens.neutralButtonFill,
            iconTint = if (state.localVideoOn) tokens.onAccent else tokens.textPrimary,
            size = 52.dp,
            onClick = onToggleCamera,
        )
    }
    if (state.localVideoOn && featureEnabled(FeatureFlag.CALLS_FLIP_CAMERA)) {
        CallControlButton(
            Icons.Default.Cameraswitch,
            "Flip",
            tokens.neutralButtonFill,
            iconTint = tokens.textPrimary,
            size = 52.dp,
            onClick = onSwitchCamera,
        )
    }
    CallControlButton(
        Icons.Default.MicOff,
        "Mute",
        if (state.muted) tokens.accent else tokens.neutralButtonFill,
        iconTint = if (state.muted) tokens.onAccent else tokens.textPrimary,
        size = 52.dp,
        onClick = onToggleMute,
    )
    CallControlButton(Icons.Default.CallEnd, "End", tokens.danger, size = 64.dp, onClick = onEnd)
}
