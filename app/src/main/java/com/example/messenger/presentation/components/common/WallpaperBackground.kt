package com.example.messenger.presentation.components.common
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import coil3.compose.AsyncImage
import com.example.messenger.di.UiEntryPoints
import com.example.messenger.presentation.screens.ui.theme.appBackgroundFor
import com.example.messenger.presentation.screens.ui.theme.customWallpaperFile
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import dagger.hilt.android.EntryPointAccessors

@Composable
fun WallpaperBackdrop(modifier: Modifier = Modifier) {
    val tokens = messengerTokens
    val inspection = LocalInspectionMode.current
    var backgroundKey = "neutral"
    var intensity = 1f
    if (!inspection) {
        val context = LocalContext.current
        val store = remember {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                UiEntryPoints::class.java,
            ).themePreferenceStore()
        }
        backgroundKey = store.background.collectAsState(initial = "neutral").value
        intensity = store.intensity.collectAsState(initial = 1f).value
    }
    val preset = appBackgroundFor(backgroundKey)
    val colors = if (tokens.isDark) preset.darkColors else preset.lightColors
    val customFile = if (inspection) {
        null
    } else {
        val context = LocalContext.current
        remember(backgroundKey) {
            customWallpaperFile(context, backgroundKey)?.takeIf { it.exists() }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors)),
        )
        if (customFile != null) {
            AsyncImage(
                model = customFile,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        val scrimAlpha = ((1f - intensity) * 0.85f).coerceIn(0f, 1f)
        if (scrimAlpha > 0.005f) {
            val scrim = if (tokens.isDark) Color.Black else Color.White
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrim.copy(alpha = scrimAlpha)),
            )
        }
    }
}

@Composable
fun WallpaperBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        WallpaperBackdrop()
        content()
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WallpaperBackgroundPreview() {
    MessengerTheme {
        WallpaperBackground {
            Text(
                "Wallpaper",
                modifier = Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
