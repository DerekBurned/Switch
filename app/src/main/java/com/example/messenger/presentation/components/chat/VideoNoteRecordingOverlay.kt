package com.example.messenger.presentation.components.chat

import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.viewinterop.AndroidView
import com.example.messenger.data.media.VideoRecordState
import com.example.messenger.domain.model.voicemessage.Clip
import com.example.messenger.presentation.theme.messengerTokens

@Composable
fun VideoNoteRecordingOverlay(
    state: VideoRecordState.Recording,
    onAttachPreview: (Preview.SurfaceProvider) -> Unit,
    onDetachPreview: (Preview.SurfaceProvider) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val tokens = messengerTokens
    val context = LocalContext.current
    val attach by rememberUpdatedState(onAttachPreview)
    val detach by rememberUpdatedState(onDetachPreview)
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    DisposableEffect(previewView) {
        val provider = previewView.surfaceProvider
        attach(provider)
        onDispose { detach(provider) }
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(bottom = bottomInset),
        contentAlignment = Alignment.Center,
    ) {
        val diameter = min(maxWidth * 0.78f, MAX_DIAMETER).coerceAtMost(maxHeight * 0.8f)
        val fraction = (state.elapsedMs.toFloat() / Clip.Video.MAX_DURATION_MS).coerceIn(0f, 1f)
        Box(modifier = Modifier.size(diameter), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .size(diameter - RING_GAP * 2)
                    .clip(CircleShape)
                    .background(tokens.cardFill),
            )
            Canvas(modifier = Modifier.size(diameter)) {
                val stroke = RING_STROKE.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = Color.White.copy(alpha = 0.25f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
                if (fraction > 0f) {
                    drawArc(
                        color = tokens.danger,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            if (!state.started) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp,
                    color = Color.White,
                )
            }
        }
    }
}

private val MAX_DIAMETER = 340.dp
private val RING_STROKE = 4.dp
private val RING_GAP = 6.dp
