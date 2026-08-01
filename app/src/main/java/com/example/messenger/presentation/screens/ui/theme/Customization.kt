package com.example.messenger.presentation.screens.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.font.FontFamily

enum class BarPosition {
    TOP,
    BOTTOM,
}

enum class AppTextSize(val scale: Float, val label: String) {
    SMALL(0.9f, "Small"),
    MEDIUM(1f, "Medium"),
    LARGE(1.15f, "Large"),
}

enum class AppFont(val label: String) {
    DEFAULT("Default"),
    SERIF("Serif"),
    MONO("Mono"),
}

fun AppFont.toFontFamily(): FontFamily = when (this) {
    AppFont.DEFAULT -> Inter
    AppFont.SERIF -> FontFamily.Serif
    AppFont.MONO -> FontFamily.Monospace
}

/** Where the floating tab/search bar is docked. Provided by [MainActivity]. */
val LocalTabBarPosition = compositionLocalOf { BarPosition.TOP }
