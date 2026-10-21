package com.example.messenger.presentation.components.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import com.example.messenger.data.media.normalizeWaveform
import kotlin.math.max

@Composable
fun VoiceWaveform(
    waveform: List<Int>,
    progress: () -> Float,
    playedColor: Color,
    idleColor: Color,
    modifier: Modifier = Modifier,
    onSeek: ((Float) -> Unit)? = null,
) {
    val bars = remember(waveform) { normalizeWaveform(waveform) }.ifEmpty { List(FALLBACK_BARS) { FALLBACK_LEVEL } }
    val seekModifier = if (onSeek == null) {
        modifier
    } else {
        modifier
            .pointerInput(bars.size) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(bars.size) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                }
            }
    }
    Canvas(modifier = seekModifier) {
        if (bars.isEmpty()) return@Canvas
        drawBars(bars, idleColor)
        val playedUpTo = size.width * progress().coerceIn(0f, 1f)
        if (playedUpTo > 0f) {
            clipRect(right = playedUpTo) { drawBars(bars, playedColor) }
        }
    }
}

private fun DrawScope.drawBars(bars: List<Int>, color: Color) {
    val slot = size.width / bars.size
    val barWidth = max(slot * BAR_RATIO, MIN_BAR_WIDTH_PX)
    bars.forEachIndexed { index, level ->
        val fraction = (level.coerceIn(0, 100) / 100f).coerceAtLeast(MIN_LEVEL)
        val barHeight = size.height * fraction
        drawRoundRect(
            color = color,
            topLeft = Offset(index * slot + (slot - barWidth) / 2f, (size.height - barHeight) / 2f),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2f),
        )
    }
}

private const val FALLBACK_BARS = 32
private const val FALLBACK_LEVEL = 18
private const val BAR_RATIO = 0.55f
private const val MIN_BAR_WIDTH_PX = 2f
private const val MIN_LEVEL = 0.12f
