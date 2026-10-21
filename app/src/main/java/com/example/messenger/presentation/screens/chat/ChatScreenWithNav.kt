package com.example.messenger.presentation.screens.chat

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.R
import com.example.messenger.data.media.GalleryItem
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.voicemessage.ClipKind
import com.example.messenger.presentation.base.ObserveAsEvents
import com.example.messenger.presentation.components.media.FullscreenMediaPager
import com.example.messenger.presentation.components.media.model.MediaSource
import com.example.messenger.presentation.featureflag.featureEnabled
import com.example.messenger.presentation.notification.ChatNotifier
import com.example.messenger.presentation.notification.CurrentConversationHolder
import com.example.messenger.presentation.screens.mediapicker.MediaPickerBottomSheet
import com.example.messenger.util.PermissionUtils
import kotlinx.coroutines.launch

private fun recordPermissions(video: Boolean): Array<String> =
    if (video) {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    } else {
        arrayOf(Manifest.permission.RECORD_AUDIO)
    }

private fun mediaReadPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenWithNav(
    viewModel: ChatViewModel = hiltViewModel(),
    sharedKeyPartnerId: String = "",
    inBubble: Boolean = false,
    onBackClick: () -> Unit = {},
    onCallClick: () -> Unit = {},
    onVideoCallClick: () -> Unit = {},
    onIntercultorProfileClick: (avatarUrl: String?, partnerName: String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val clipPlayback = viewModel.clipPlayback.collectAsStateWithLifecycle()
    var messageText by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current
    ObserveAsEvents(viewModel.effect) { effect ->
        when (effect) {
            is ChatEffect.ShowError -> Toast.makeText(context, effect.message.asString(context), Toast.LENGTH_SHORT).show()
        }
    }

    var showPicker by remember { mutableStateOf(false) }
    var cameraGranted by remember { mutableStateOf(false) }
    var devicePreview by remember { mutableStateOf<Pair<List<GalleryItem>, Int>?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraGranted = granted
        Log.d("ChatScreen", "camera permission granted=$granted")
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val anyGranted = result.values.any { it }
        Log.d("ChatScreen", "media permission result granted=$anyGranted")
        if (anyGranted) showPicker = true
    }

    val activity = LocalActivity.current
    var rationaleForVideo by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var requestedForVideo by rememberSaveable { mutableStateOf(false) }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val denied = result.filterValues { granted -> !granted }.keys
        if (denied.isNotEmpty()) {
            val permanently = activity == null ||
                denied.none { PermissionUtils.shouldShowRationale(activity, it) }
            viewModel.dispatch(ChatIntent.MicPermissionDenied(permanently, requestedForVideo))
        }
    }

    rationaleForVideo?.let { video ->
        MicPermissionDialog(
            title = if (video) R.string.camera_permission_title else R.string.voice_permission_title,
            body = if (video) R.string.camera_permission_rationale else R.string.voice_permission_rationale,
            confirm = R.string.voice_permission_allow,
            onConfirm = {
                rationaleForVideo = null
                requestedForVideo = video
                recordPermissionLauncher.launch(recordPermissions(video))
            },
            onDismiss = { rationaleForVideo = null },
        )
    }

    if (uiState.micDenied) {
        val video = uiState.micDeniedForVideo
        MicPermissionDialog(
            title = if (video) R.string.camera_permission_title else R.string.voice_permission_title,
            body = if (video) R.string.camera_permission_settings else R.string.voice_permission_settings,
            confirm = R.string.voice_permission_open_settings,
            onConfirm = {
                viewModel.dispatch(ChatIntent.DismissMicPrompt)
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
                runCatching { context.startActivity(intent) }
            },
            onDismiss = { viewModel.dispatch(ChatIntent.DismissMicPrompt) },
        )
    }

    val conversationId = viewModel.conversationId
    DisposableEffect(conversationId) {
        if (conversationId.isNotBlank()) {
            CurrentConversationHolder.setOpen(conversationId)
            if (!inBubble) ChatNotifier.clear(context, conversationId)
        }
        onDispose {
            if (conversationId.isNotBlank()) {
                CurrentConversationHolder.clear(conversationId)
            }
            if (activity?.isChangingConfigurations != true) {
                viewModel.dispatch(ChatIntent.InterruptRecording)
            }
        }
    }

    ChatScreenContent(
        uiState = uiState,
        sharedKeyPartnerId = sharedKeyPartnerId,
        messageText = messageText,
        onMessageTextChange = { newText ->
            messageText = newText
            viewModel.dispatch(ChatIntent.TextChanged(newText))
        },
        onSendClick = {
            if (messageText.isNotBlank() || uiState.pendingAttachments.isNotEmpty()) {
                viewModel.dispatch(ChatIntent.SendMessage(messageText))
                messageText = ""
            }
        },
        onBackClick = onBackClick,
        onCopy = { text ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
        },
        onReply = { message -> viewModel.dispatch(ChatIntent.SetReplyTo(message)) },
        onReplyClick = { messageId -> viewModel.dispatch(ChatIntent.JumpToMessage(messageId)) },
        onDelete = { message, forEveryone ->
            if (!forEveryone || message.senderId == uiState.currentUserId) {
                viewModel.dispatch(ChatIntent.DeleteMessage(message, forEveryone))
            }
        },
        onClearReply = { viewModel.dispatch(ChatIntent.ClearReply) },
        onAttachmentClick = {
            val perms = mediaReadPermissions()
            val granted = perms.any {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (granted) {
                showPicker = true
            } else {
                mediaPermissionLauncher.launch(perms)
            }
        },
        onClearAttachments = { viewModel.dispatch(ChatIntent.ClearAttachments) },
        onIntercultorProfileClick = onIntercultorProfileClick,
        onCallClick = onCallClick,
        onVideoCallClick = onVideoCallClick,
        onMessagesSeen = { messages -> viewModel.dispatch(ChatIntent.MessagesSeen(messages)) },
        onLoadOlderMessages = { viewModel.dispatch(ChatIntent.LoadOlderMessages) },
        onDownload = { item -> viewModel.dispatch(ChatIntent.DownloadMedia(item)) },
        onCancelUpload = { messageId, itemId -> viewModel.dispatch(ChatIntent.CancelUpload(messageId, itemId)) },
        onCancelDownload = { itemId -> viewModel.dispatch(ChatIntent.CancelDownload(itemId)) },
        onRetryMedia = { messageId -> viewModel.dispatch(ChatIntent.RetryMedia(messageId)) },
        onPlayClip = { messageId -> viewModel.dispatch(ChatIntent.PlayClip(messageId)) },
        onSeekClip = { fraction -> viewModel.dispatch(ChatIntent.SeekClip(fraction)) },
        clipPlayback = clipPlayback,
        onRetryClip = { messageId -> viewModel.dispatch(ChatIntent.RetryClip(messageId)) },
        onCancelClipSend = { messageId -> viewModel.dispatch(ChatIntent.CancelClipSend(messageId)) },
        onStartRecording = {
            val video = uiState.clipMode == ClipKind.VIDEO
            val missing = recordPermissions(video).filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            when {
                missing.isEmpty() -> viewModel.dispatch(ChatIntent.StartRecording)
                activity != null && missing.any { PermissionUtils.shouldShowRationale(activity, it) } ->
                    rationaleForVideo = video
                else -> {
                    requestedForVideo = video
                    recordPermissionLauncher.launch(missing.toTypedArray())
                }
            }
        },
        onToggleClipMode = { viewModel.dispatch(ChatIntent.ToggleClipMode) },
        onAttachVideoPreview = viewModel::attachVideoPreview,
        onDetachVideoPreview = viewModel::detachVideoPreview,
        videoFrame = viewModel::clipVideoFrame,
        onAttachClipVideo = viewModel::attachClipVideo,
        onDetachClipVideo = viewModel::detachClipVideo,
        onLockRecording = { viewModel.dispatch(ChatIntent.LockRecording) },
        onCancelRecording = { viewModel.dispatch(ChatIntent.CancelRecording) },
        onFinishRecording = { viewModel.dispatch(ChatIntent.FinishRecording) },
        onInterruptRecording = { viewModel.dispatch(ChatIntent.InterruptRecording) },
        onSendVoiceDraft = { viewModel.dispatch(ChatIntent.SendVoiceDraft) },
        onDiscardVoiceDraft = { viewModel.dispatch(ChatIntent.DiscardVoiceDraft) },
    )

    val cameraCellEnabled = featureEnabled(FeatureFlag.MEDIA_CAMERA)
    if (showPicker) {
        LaunchedEffect(Unit) {
            if (!cameraCellEnabled) return@LaunchedEffect
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                cameraGranted = true
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        MediaPickerBottomSheet(
            selected = uiState.pendingAttachments.map { it.uri }.toSet(),
            onToggle = { uri, kind -> viewModel.dispatch(ChatIntent.ToggleAttachment(uri, kind)) },
            onPreview = { items, index -> devicePreview = items to index },
            onCaptured = { uri, kind -> viewModel.dispatch(ChatIntent.AddAttachment(uri, kind)) },
            onDone = { showPicker = false },
            onDismiss = { showPicker = false },
            cameraEnabled = cameraGranted,
        )
    }

    devicePreview?.let { (items, index) ->
        val sources = remember(items) {
            items.map { gi ->
                MediaSource(
                    key = gi.id.toString(),
                    isVideo = gi.kind == MediaItem.VIDEO,
                    model = gi.uri,
                    playUri = gi.uri,
                )
            }
        }
        FullscreenMediaPager(
            sources = sources,
            startIndex = index,
            onDismiss = { devicePreview = null },
        )
    }
}

@Composable
private fun MicPermissionDialog(
    @StringRes title: Int,
    @StringRes body: Int,
    @StringRes confirm: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.voice_permission_not_now)) }
        },
    )
}
