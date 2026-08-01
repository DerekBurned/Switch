package com.example.messenger.presentation.screens.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

object Motion {
    const val durationShort = 180
    const val durationMedium = 280
    const val durationLong = 420

    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val standard: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
}
