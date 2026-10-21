package com.example.messenger.presentation.contrast

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

internal const val WCAG_NORMAL_TEXT = 4.5
internal const val WCAG_LARGE_TEXT = 3.0

internal fun relativeLuminance(color: Color): Double {
    fun linear(component: Float): Double {
        val value = component.toDouble()
        return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * linear(color.red) +
        0.7152 * linear(color.green) +
        0.0722 * linear(color.blue)
}

internal fun contrastRatio(first: Color, second: Color): Double {
    val a = relativeLuminance(first)
    val b = relativeLuminance(second)
    return (max(a, b) + 0.05) / (min(a, b) + 0.05)
}
