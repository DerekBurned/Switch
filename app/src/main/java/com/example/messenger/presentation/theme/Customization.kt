package com.example.messenger.presentation.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import com.example.messenger.presentation.theme.model.AppFont
import com.example.messenger.presentation.theme.model.BarPosition

fun AppFont.toFontFamily(): FontFamily = when (this) {
    AppFont.DEFAULT -> Inter
    AppFont.SERIF -> FontFamily.Serif
    AppFont.MONO -> FontFamily.Monospace
}

/** Where the floating tab/search bar is docked. Provided by [MainActivity]. */
val LocalTabBarPosition = compositionLocalOf { BarPosition.TOP }
