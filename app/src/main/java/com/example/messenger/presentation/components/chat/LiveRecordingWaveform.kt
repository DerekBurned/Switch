package com.example.messenger.presentation.components.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.messenger.data.media.VOICE_SAMPLE_INTERVAL_MS
import kotlinx.coroutines.launch
import kotlin.math.max

internal fun liveWaveformOffset(barCount: Int, pitchPx: Float, widthPx: Float): Float =
    (barCount * pitchPx - widthPx).coerceAtLeast(0f)

@Composable
fun LiveRecordingWaveform(
    amplitudes: List<Int>,
    sampleCount: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val barPx = with(density) { BAR_WIDTH.toPx() }
    val pitchPx = barPx + with(density) { BAR_GAP.toPx() }
    var widthPx by remember { mutableFloatStateOf(0f) }
    val scroll = remember { Animatable(0f) }
    val appear = remember { Animatable(1f) }
    LaunchedEffect(sampleCount, widthPx, pitchPx) {
        val target = liveWaveformOffset(sampleCount, pitchPx, widthPx)
        launch {
            appear.snapTo(0f)
            appear.animateTo(1f, tween(STEP_MS, easing = LinearEasing))
        }
        if (target <= scroll.value) {
            scroll.snapTo(target)
        } else {
            scroll.animateTo(target, tween(STEP_MS, easing = LinearEasing))
        }
    }
    Canvas(modifier = modifier.onSizeChanged { widthPx = it.width.toFloat() }) {
        if (amplitudes.isEmpty()) return@Canvas
        val firstIndex = sampleCount - amplitudes.size
        val offset = scroll.value
        val lastIndex = amplitudes.lastIndex
        amplitudes.forEachIndexed { index, level ->
            val x = (firstIndex + index) * pitchPx - offset
            if (x < -pitchPx || x > size.width) return@forEachIndexed
            val growth = if (index == lastIndex) appear.value else 1f
            val fraction = max(level.coerceIn(0, 100) / 100f, MIN_FRACTION) * growth
            val barHeight = size.height * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset(x, (size.height - barHeight) / 2f),
                size = Size(barPx, barHeight),
                cornerRadius = CornerRadius(barPx / 2f),
            )
        }
    }
}

private val BAR_WIDTH = 3.dp
private val BAR_GAP = 2.dp
private const val MIN_FRACTION = 0.08f
private val STEP_MS = VOICE_SAMPLE_INTERVAL_MS.toInt()
