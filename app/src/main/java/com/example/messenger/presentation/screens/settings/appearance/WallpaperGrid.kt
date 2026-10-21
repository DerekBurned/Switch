package com.example.messenger.presentation.screens.settings.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.messenger.presentation.theme.backgroundKeyFor
import com.example.messenger.presentation.theme.messengerTokens
import com.example.messenger.presentation.theme.model.AppBackground
import com.example.messenger.presentation.theme.model.SavedWallpaper

@Composable
internal fun WallpaperGrid(
    presets: List<AppBackground>,
    saved: List<SavedWallpaper>,
    selectedKey: String,
    isDark: Boolean,
    onSelectPreset: (String) -> Unit,
    onSelectSaved: (SavedWallpaper) -> Unit,
    onAdd: () -> Unit,
    onRename: (SavedWallpaper) -> Unit,
    onDelete: (SavedWallpaper) -> Unit,
    customEnabled: Boolean = true,
) {
    val tokens = messengerTokens
    val customTiles = if (customEnabled) saved.map { GridTile.Saved(it) } + GridTile.Add else emptyList()
    val rows = (presets.map { GridTile.Preset(it) } + customTiles).chunked(3)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { tile ->
                    when (tile) {
                        is GridTile.Preset -> BackgroundTile(
                            selected = tile.preset.key == selectedKey,
                            label = tile.preset.label,
                            onClick = { onSelectPreset(tile.preset.key) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            if (isDark) tile.preset.darkColors else tile.preset.lightColors,
                                        ),
                                    ),
                            )
                        }
                        is GridTile.Saved -> SavedWallpaperTile(
                            wallpaper = tile.wallpaper,
                            selected = selectedKey == backgroundKeyFor(tile.wallpaper.id),
                            onApply = { onSelectSaved(tile.wallpaper) },
                            onRename = { onRename(tile.wallpaper) },
                            onDelete = { onDelete(tile.wallpaper) },
                            modifier = Modifier.weight(1f),
                        )
                        GridTile.Add -> BackgroundTile(
                            selected = false,
                            label = "Add",
                            onClick = onAdd,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(tokens.pillFill),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddPhotoAlternate,
                                    contentDescription = "Add wallpaper from gallery",
                                    tint = tokens.textMuted,
                                    modifier = Modifier.size(30.dp),
                                )
                            }
                        }
                    }
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private sealed interface GridTile {
    data class Preset(val preset: AppBackground) : GridTile
    data class Saved(val wallpaper: SavedWallpaper) : GridTile
    data object Add : GridTile
}
