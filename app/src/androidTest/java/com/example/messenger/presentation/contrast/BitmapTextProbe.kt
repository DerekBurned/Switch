package com.example.messenger.presentation.contrast

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

internal const val MIN_FOREGROUND_FRACTION = 0.005

internal data class TextSample(
    val foreground: Color,
    val background: Color,
    val foregroundPixels: Int,
    val totalPixels: Int,
)

internal fun Bitmap.probeTextRegion(
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    minForegroundFraction: Double = MIN_FOREGROUND_FRACTION,
): TextSample? {
    val x0 = left.coerceIn(0, width)
    val y0 = top.coerceIn(0, height)
    val x1 = right.coerceIn(0, width)
    val y1 = bottom.coerceIn(0, height)
    val cropWidth = x1 - x0
    val cropHeight = y1 - y0
    if (cropWidth <= 0 || cropHeight <= 0) return null

    val pixels = IntArray(cropWidth * cropHeight)
    getPixels(pixels, 0, cropWidth, x0, y0, cropWidth, cropHeight)

    val counts = HashMap<Int, Int>()
    for (pixel in pixels) counts[pixel] = (counts[pixel] ?: 0) + 1

    val background = counts.maxByOrNull { it.value }?.key ?: return null
    val backgroundColor = Color(background)

    if (counts.size == 1) {
        return TextSample(
            foreground = backgroundColor,
            background = backgroundColor,
            foregroundPixels = 0,
            totalPixels = pixels.size,
        )
    }

    val backgroundLuminance = relativeLuminance(backgroundColor)
    val ranked = counts.entries
        .filter { it.key != background }
        .sortedByDescending { abs(relativeLuminance(Color(it.key)) - backgroundLuminance) }

    val required = (pixels.size * minForegroundFraction).toInt().coerceAtLeast(1)
    var accumulated = 0
    for (entry in ranked) {
        accumulated += entry.value
        if (accumulated >= required) {
            return TextSample(
                foreground = Color(entry.key),
                background = backgroundColor,
                foregroundPixels = accumulated,
                totalPixels = pixels.size,
            )
        }
    }
    return null
}
