package com.example.messenger.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.messenger.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val tracking = (-0.2).sp

/**
 * Builds the app typography, scaling every size by [scale] (user text-size preference)
 * and using [family] (user font preference).
 */
fun messengerTypography(
    scale: Float = 1f,
    family: FontFamily = Inter,
): Typography {
    fun size(value: Float) = (value * scale).sp
    return Typography(
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = size(24f),
            lineHeight = size(30f),
            letterSpacing = tracking,
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = size(20f),
            lineHeight = size(26f),
            letterSpacing = tracking,
        ),
        titleSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = size(16f),
            lineHeight = size(22f),
            letterSpacing = tracking,
        ),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = size(16f),
            lineHeight = size(22f),
            letterSpacing = tracking,
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = size(15f),
            lineHeight = size(20f),
            letterSpacing = tracking,
        ),
        bodySmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = size(13f),
            lineHeight = size(18f),
            letterSpacing = tracking,
        ),
        labelLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = size(16f),
            lineHeight = size(20f),
            letterSpacing = tracking,
        ),
        labelMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = size(13f),
            lineHeight = size(16f),
            letterSpacing = tracking,
        ),
        labelSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = size(12f),
            lineHeight = size(16f),
            letterSpacing = tracking,
        ),
    )
}

val Typography = messengerTypography()
