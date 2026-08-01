package com.example.messenger.presentation.components.media

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import com.example.messenger.data.media.MediaCache
import com.example.messenger.domain.model.MediaItem
import java.io.File
import java.util.UUID

private enum class CaptureMode { PHOTO, VIDEO }

@SuppressLint("MissingPermission")
@Composable
fun CameraCaptureScreen(
    onResult: (Uri, String, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val controller = remember { LifecycleCameraController(context) }

    var mode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var isRecording by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf<Recording?>(null) }

    DisposableEffect(Unit) {
        previewView.controller = controller
        controller.setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        controller.bindToLifecycle(lifecycleOwner)
        Log.d(TAG, "controller bound")
        onDispose {
            runCatching { recording?.stop() }
            controller.unbind()
            Log.d(TAG, "controller unbound")
        }
    }

    LaunchedEffect(mode) {
        controller.setEnabledUseCases(
            if (mode == CaptureMode.PHOTO) CameraController.IMAGE_CAPTURE else CameraController.VIDEO_CAPTURE,
        )
        Log.d(TAG, "use case -> $mode")
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                ModeTab("Photo", mode == CaptureMode.PHOTO, enabled = !isRecording) {
                    mode = CaptureMode.PHOTO
                }
                ShutterButton(
                    isRecording = isRecording,
                    onClick = {
                        when (mode) {
                            CaptureMode.PHOTO -> capturePhoto(context, controller, onResult)
                            CaptureMode.VIDEO -> if (isRecording) {
                                recording?.stop()
                                recording = null
                            } else {
                                recording = startRecording(context, controller) { uri, dur ->
                                    isRecording = false
                                    onResult(uri, MediaItem.VIDEO, dur)
                                }
                                isRecording = true
                            }
                        }
                    },
                )
                ModeTab("Video", mode == CaptureMode.VIDEO, enabled = !isRecording) {
                    mode = CaptureMode.VIDEO
                }
            }
        }
    }
}

@Composable
private fun ModeTab(label: String, active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .size(width = 64.dp, height = 72.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (active) Color.White else Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun ShutterButton(isRecording: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(if (isRecording) 30.dp else 56.dp)
                .clip(if (isRecording) RoundedCornerShape(6.dp) else CircleShape)
                .background(if (isRecording) Color.Red else Color.White),
        )
    }
}

private fun capturePhoto(
    context: Context,
    controller: LifecycleCameraController,
    onResult: (Uri, String, Long) -> Unit,
) {
    val file = captureCacheFile(context, MediaItem.IMAGE)
    val options = ImageCapture.OutputFileOptions.Builder(file).build()
    controller.takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Log.d(TAG, "photo captured ${file.name}")
                onResult(Uri.fromFile(file), MediaItem.IMAGE, 0L)
            }

            override fun onError(exc: ImageCaptureException) {
                Log.e(TAG, "photo capture failed", exc)
            }
        },
    )
}

@SuppressLint("MissingPermission")
private fun startRecording(
    context: Context,
    controller: LifecycleCameraController,
    onFinalized: (Uri, Long) -> Unit,
): Recording {
    val file = captureCacheFile(context, MediaItem.VIDEO)
    val audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    val audioConfig = if (audioGranted) AudioConfig.create(true) else AudioConfig.AUDIO_DISABLED
    Log.d(TAG, "start recording ${file.name} audio=$audioGranted")
    val listener = Consumer<VideoRecordEvent> { event ->
        if (event is VideoRecordEvent.Finalize) {
            if (event.hasError()) {
                Log.e(TAG, "video record error=${event.error}")
            } else {
                val durationMs = event.recordingStats.recordedDurationNanos / 1_000_000
                Log.d(TAG, "video finalized ${file.name} duration=${durationMs}ms")
                onFinalized(Uri.fromFile(file), durationMs)
            }
        }
    }
    return controller.startRecording(
        FileOutputOptions.Builder(file).build(),
        audioConfig,
        ContextCompat.getMainExecutor(context),
        listener,
    )
}

private fun captureCacheFile(context: Context, kind: String): File =
    File(
        File(context.cacheDir, MediaCache.DIR).apply { mkdirs() },
        MediaCache.fileName(UUID.randomUUID().toString(), kind),
    )

private const val TAG = "CameraCaptureScreen"
