package com.example.messenger.presentation.screens.call

import android.graphics.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.domain.service.ICallService
import com.example.messenger.presentation.components.call.CallVideoView
import com.example.messenger.presentation.components.call.PipSourceRect
import com.example.messenger.presentation.components.call.VideoSurfaceOwner
import com.example.messenger.presentation.components.call.model.SurfaceKind
import com.example.messenger.presentation.components.call.model.VideoStream
import com.example.messenger.presentation.components.common.springEnter
import com.example.messenger.presentation.theme.messengerTokens
import kotlin.math.roundToInt

@Composable
internal fun VideoCallContent(
    state: CallUiState,
    micDenied: Boolean,
    onBack: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onEnd: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    callService: ICallService?,
) {
    val tokens = messengerTokens
    var parentSize by remember { mutableStateOf(IntSize.Zero) }
    val remoteStream = state.remoteUid?.let { VideoStream.Remote(it) }

    DisposableEffect(Unit) {
        onDispose { PipSourceRect.clear() }
    }

    val videoBounds = Modifier
        .fillMaxSize()
        .onGloballyPositioned { coords ->
            val bounds = coords.boundsInWindow()
            PipSourceRect.set(
                Rect(
                    bounds.left.roundToInt(),
                    bounds.top.roundToInt(),
                    bounds.right.roundToInt(),
                    bounds.bottom.roundToInt(),
                ),
            )
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { parentSize = it },
    ) {
        if (callService != null) {
            when {
                state.remoteVideoOn && remoteStream != null -> key(remoteStream) {
                    CallVideoView(
                        host = VideoSurfaceOwner.Host.FULLSCREEN,
                        stream = remoteStream,
                        service = callService,
                        kind = SurfaceKind.SURFACE,
                        modifier = videoBounds,
                    )
                }
                state.localVideoOn -> key(VideoStream.Local) {
                    CallVideoView(
                        host = VideoSurfaceOwner.Host.FULLSCREEN,
                        stream = VideoStream.Local,
                        service = callService,
                        kind = SurfaceKind.SURFACE,
                        modifier = videoBounds,
                    )
                }
            }
        }

        if (!state.remoteVideoOn && !state.localVideoOn) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .springEnter(initialScale = 0.88f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CallAvatar(name = state.partnerName, photoUrl = state.partnerAvatarUrl)
            }
        }

        CallBackButton(
            onBack = onBack,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 10.dp, top = 12.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.partnerName.ifBlank { "Calling…" },
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = callStatusText(state),
                color = Color.White.copy(0.8f),
                fontSize = 13.sp,
            )
            if (micDenied) {
                Text(
                    text = "Microphone permission is required to talk.",
                    color = tokens.danger,
                    fontSize = 12.sp,
                )
            }
        }

        if (state.remoteVideoOn && state.localVideoOn && callService != null) {
            DraggableLocalPreview(
                parentSize = parentSize,
                callService = callService,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            CallControlsRow(
                state = state,
                onAccept = onAccept,
                onDecline = onDecline,
                onEnd = onEnd,
                onToggleSpeaker = onToggleSpeaker,
                onToggleMute = onToggleMute,
                onToggleCamera = onToggleCamera,
                onSwitchCamera = onSwitchCamera,
            )
        }
    }
}
