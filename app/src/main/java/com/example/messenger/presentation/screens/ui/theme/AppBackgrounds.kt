package com.example.messenger.presentation.screens.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import java.io.File

data class AppBackground(
    val key: String,
    val label: String,
    val darkColors: List<Color>,
    val lightColors: List<Color>,
)

val AppBackgrounds = listOf(
    AppBackground(
        key = "neutral",
        label = "Neutral",
        darkColors = listOf(Color(0xFF1C1A20), Color(0xFF16141B)),
        lightColors = listOf(Color(0xFFEDE7F4), Color(0xFFD7CEE6)),
    ),
    AppBackground(
        key = "aurora",
        label = "Aurora",
        darkColors = listOf(Color(0xFF2A1B47), Color(0xFF15212B), Color(0xFF0E1A1F)),
        lightColors = listOf(Color(0xFFDCD3F0), Color(0xFFC7D8E4), Color(0xFFC2D6D2)),
    ),
    AppBackground(
        key = "plum",
        label = "Plum",
        darkColors = listOf(Color(0xFF3A1F3D), Color(0xFF1B1622)),
        lightColors = listOf(Color(0xFFE8D3EA), Color(0xFFD5CCE0)),
    ),
    AppBackground(
        key = "teal",
        label = "Teal",
        darkColors = listOf(Color(0xFF123233), Color(0xFF0E1A1F)),
        lightColors = listOf(Color(0xFFCBE5E5), Color(0xFFC5D6DB)),
    ),
    AppBackground(
        key = "midnight",
        label = "Midnight",
        darkColors = listOf(Color(0xFF1C2A4A), Color(0xFF0E1320)),
        lightColors = listOf(Color(0xFFCFD9EE), Color(0xFFC2C9DC)),
    ),
    AppBackground(
        key = "mono",
        label = "Mono",
        darkColors = listOf(Color(0xFF241F2C), Color(0xFF141118)),
        lightColors = listOf(Color(0xFFE2DEE8), Color(0xFFCFCAD6)),
    ),
)

fun appBackgroundFor(key: String?): AppBackground =
    AppBackgrounds.firstOrNull { it.key == key } ?: AppBackgrounds.first()

const val CUSTOM_BACKGROUND_PREFIX = "custom:"

/** A user-imported wallpaper the app has saved. [id] maps to a file, [name] is user-editable. */
@Serializable
data class SavedWallpaper(
    val id: String,
    val name: String,
)

fun backgroundKeyFor(wallpaperId: String): String = "$CUSTOM_BACKGROUND_PREFIX$wallpaperId"

fun wallpaperIdOf(backgroundKey: String): String? =
    if (backgroundKey.startsWith(CUSTOM_BACKGROUND_PREFIX)) {
        backgroundKey.removePrefix(CUSTOM_BACKGROUND_PREFIX)
    } else {
        null
    }

fun wallpaperFileById(context: Context, wallpaperId: String): File =
    File(File(context.filesDir, "wallpapers"), "wallpaper_$wallpaperId.jpg")

fun customWallpaperFile(context: Context, backgroundKey: String): File? {
    val id = wallpaperIdOf(backgroundKey) ?: return null
    return wallpaperFileById(context, id)
}
