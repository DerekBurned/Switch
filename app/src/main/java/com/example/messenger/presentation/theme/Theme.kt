package com.example.messenger.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.messenger.presentation.theme.model.AppFont
import com.example.messenger.presentation.theme.model.AppTextSize

private val DarkColorScheme = darkColorScheme(
    primary = AccentTeal,
    onPrimary = PillWhite,
    secondary = AccentTeal,
    background = BgDarkBottom,
    onBackground = TextDarkPrimary,
    surface = BgDarkTop,
    onSurface = TextDarkPrimary,
    surfaceVariant = CardFillDark,
    onSurfaceVariant = LabelMutedDark,
    error = DeclineRed,
    onError = PillWhite,
    outline = LabelMutedDark,
)

private val LightColorScheme = lightColorScheme(
    primary = AccentTeal,
    onPrimary = PillWhite,
    secondary = AccentTeal,
    background = BgLightBottom,
    onBackground = TextLightPrimary,
    surface = BgLightTop,
    onSurface = TextLightPrimary,
    surfaceVariant = CardFillLight,
    onSurfaceVariant = TextLightMuted,
    error = DeclineRed,
    onError = PillWhite,
    outline = TextLightMuted,
)

@Composable
fun MessengerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    textSize: AppTextSize = AppTextSize.MEDIUM,
    appFont: AppFont = AppFont.DEFAULT,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val tokens = if (darkTheme) DarkTokens else LightTokens
    val typography = messengerTypography(scale = textSize.scale, family = appFont.toFontFamily())

    CompositionLocalProvider(
        LocalMessengerTokens provides tokens,
        LocalContentColor provides tokens.textPrimary,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = MessengerMaterialShapes,
            content = content,
        )
    }
}
