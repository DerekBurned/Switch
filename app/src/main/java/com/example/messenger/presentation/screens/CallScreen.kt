package com.example.messenger.presentation.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.SurfaceView
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.example.messenger.presentation.components.common.WallpaperBackground
import com.example.messenger.presentation.components.common.MessengerAvatar
import com.example.messenger.presentation.components.common.springEnter
import com.example.messenger.presentation.components.call.CallControlButton
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import com.example.messenger.presentation.screens.ui.theme.MessengerShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.domain.service.CallConnectionState
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme
import com.example.messenger.presentation.state.CallUiState
import com.example.messenger.presentation.viewmodel.CallExit
import com.example.messenger.presentation.viewmodel.CallViewModel
import kotlin.math.roundToInt

@Composable
fun CallScreen(
    viewModel: CallViewModel = hiltViewModel(),
    onCallEnded: () -> Unit = {},
    onBack: () -> Unit = {},
    onOpenChat: (conversationId: String, partnerId: String, partnerName: String) -> Unit = { _, _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions = rememberCallPermissions()

    LaunchedEffect(viewModel) {
        if (viewModel.needsOutgoingStart) {
            permissions.requestForCall(video = state.isVideoCall) { viewModel.startOutgoing() }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.callExit.collect { exit ->
            when (exit) {
                CallExit.Pop -> onCallEnded()
                is CallExit.OpenChat -> onOpenChat(exit.conversationId, exit.partnerId, exit.partnerName)
            }
        }
    }

    CallScreenContent(
        state = state,
        micDenied = permissions.micDenied,
        onBack = onBack,
        onAccept = { permissions.requestForCall(video = state.isVideoCall) { viewModel.acceptCall() } },
        onDecline = viewModel::declineCall,
        onEnd = viewModel::endCall,
        onToggleSpeaker = viewModel::toggleSpeaker,
        onToggleMute = viewModel::toggleMute,
        onToggleCamera = {
            if (state.localVideoOn) viewModel.toggleCamera()
            else permissions.requestCamera { viewModel.toggleCamera() }
        },
        onSwitchCamera = viewModel::switchCamera,
        bindLocalVideo = viewModel::bindLocalVideo,
        bindRemoteVideo = viewModel::bindRemoteVideo,
    )
}

@Composable
private fun rememberCallPermissions(): CallPermissions {
    val context = LocalContext.current
    val permissions = remember { CallPermissions(context) }
    permissions.callLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
        permissions::onCallPermissionsResult,
    )
    permissions.cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
        permissions::onCameraPermissionResult,
    )
    return permissions
}

private class CallPermissions(private val context: Context) {
    lateinit var callLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
    lateinit var cameraLauncher: ManagedActivityResultLauncher<String, Boolean>

    var micDenied by mutableStateOf(false)
        private set

    private var pendingCallAction: (() -> Unit)? = null
    private var pendingCameraAction: (() -> Unit)? = null

    fun requestForCall(video: Boolean, action: () -> Unit) {
        val needed = buildList {
            if (!granted(Manifest.permission.RECORD_AUDIO)) add(Manifest.permission.RECORD_AUDIO)
            if (video && !granted(Manifest.permission.CAMERA)) add(Manifest.permission.CAMERA)
        }
        if (needed.isEmpty()) {
            micDenied = false
            action()
        } else {
            pendingCallAction = action
            callLauncher.launch(needed.toTypedArray())
        }
    }

    fun requestCamera(action: () -> Unit) {
        if (granted(Manifest.permission.CAMERA)) {
            action()
        } else {
            pendingCameraAction = action
            cameraLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun onCallPermissionsResult(grants: Map<String, Boolean>) {
        if (grants[Manifest.permission.RECORD_AUDIO] == true) {
            micDenied = false
            pendingCallAction?.invoke()
        } else {
            micDenied = true
        }
        pendingCallAction = null
    }

    fun onCameraPermissionResult(isGranted: Boolean) {
        if (isGranted) pendingCameraAction?.invoke()
        pendingCameraAction = null
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun CallScreenContent(
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
    bindLocalVideo: (SurfaceView) -> Unit = {},
    bindRemoteVideo: (SurfaceView) -> Unit = {},
) {
    val videoMode = !state.callEnded && (state.localVideoOn || state.remoteVideoOn)
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
            bindLocalVideo = bindLocalVideo,
            bindRemoteVideo = bindRemoteVideo,
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

@Composable
private fun CallBackButton(
    onBack: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = tint,
        )
    }
}

@Composable
private fun AudioCallContent(
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

@Composable
private fun VideoCallContent(
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
    bindLocalVideo: (SurfaceView) -> Unit,
    bindRemoteVideo: (SurfaceView) -> Unit,
) {
    val tokens = messengerTokens
    var parentSize by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { parentSize = it },
    ) {
        when {
            state.remoteVideoOn -> {
                VideoSurface(
                    bind = bindRemoteVideo,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            state.localVideoOn -> {
                VideoSurface(
                    bind = bindLocalVideo,
                    modifier = Modifier.fillMaxSize(),
                )
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

        if (state.remoteVideoOn && state.localVideoOn) {
            DraggableLocalPreview(
                parentSize = parentSize,
                bindLocalVideo = bindLocalVideo,
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

@Composable
private fun DraggableLocalPreview(
    parentSize: IntSize,
    bindLocalVideo: (SurfaceView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val tileWidth = 110.dp
    val tileHeight = 160.dp
    val margin = 16.dp
    var offset by remember { mutableStateOf(Offset.Zero) }

    val tileWidthPx = with(density) { tileWidth.toPx() }
    val tileHeightPx = with(density) { tileHeight.toPx() }
    val marginPx = with(density) { margin.toPx() }
    val minX = -(parentSize.width - tileWidthPx - 2 * marginPx).coerceAtLeast(0f)
    val maxY = (parentSize.height - tileHeightPx - 2 * marginPx).coerceAtLeast(0f)

    VideoSurface(
        bind = bindLocalVideo,
        overlay = true,
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 72.dp)
            .padding(margin)
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .size(tileWidth, tileHeight)
            .clip(RoundedCornerShape(14.dp))
            .pointerInput(minX, maxY) {
                detectDragGestures { change, drag ->
                    change.consume()
                    offset = Offset(
                        (offset.x + drag.x).coerceIn(minX, 0f),
                        (offset.y + drag.y).coerceIn(0f, maxY),
                    )
                }
            },
    )
}

@Composable
private fun VideoSurface(
    bind: (SurfaceView) -> Unit,
    modifier: Modifier = Modifier,
    overlay: Boolean = false,
) {
    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                if (overlay) setZOrderMediaOverlay(true)
                bind(this)
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun CallControlsRow(
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

@Composable
private fun RowScope.IncomingCallControls(
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

@Composable
private fun RowScope.ActiveCallControls(
    state: CallUiState,
    onEnd: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
) {
    val tokens = messengerTokens
    CallControlButton(
        Icons.Default.VolumeUp,
        "Speaker",
        if (state.speakerOn) tokens.accent else tokens.neutralButtonFill,
        iconTint = if (state.speakerOn) tokens.onAccent else tokens.textPrimary,
        size = 52.dp,
        onClick = onToggleSpeaker,
    )
    CallControlButton(
        if (state.localVideoOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
        "Video",
        if (state.localVideoOn) tokens.accent else tokens.neutralButtonFill,
        iconTint = if (state.localVideoOn) tokens.onAccent else tokens.textPrimary,
        size = 52.dp,
        onClick = onToggleCamera,
    )
    if (state.localVideoOn) {
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

@Composable
private fun CallAvatar(name: String, photoUrl: String? = null) {
    MessengerAvatar(name = name, photoUrl = photoUrl, size = 121.dp)
}


private fun formatSeconds(s: Int): String {
    val m = s / 60
    val sec = s % 60
    return "$m:${sec.toString().padStart(2, '0')}"
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CallScreenIncomingPreview() {
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
fun CallScreenIncomingVideoPreview() {
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
fun CallScreenActivePreview() {
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
fun CallScreenRingingPreview() {
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
fun CallScreenRequestingPreview() {
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
