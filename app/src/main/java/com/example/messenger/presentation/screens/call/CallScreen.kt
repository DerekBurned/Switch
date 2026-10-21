package com.example.messenger.presentation.screens.call

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.R

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
            if (viewModel.outgoingAllowed) {
                permissions.requestForCall(video = state.isVideoCall) { viewModel.startOutgoing() }
            } else {
                viewModel.startOutgoing()
            }
        }
    }

    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.callExit.collect { exit ->
            when (exit) {
                CallExit.Pop -> onCallEnded()
                CallExit.Unavailable -> {
                    Toast.makeText(context, R.string.calls_unavailable, Toast.LENGTH_SHORT).show()
                    onCallEnded()
                }
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
        callService = viewModel.videoService,
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
