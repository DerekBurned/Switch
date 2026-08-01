package com.example.messenger.presentation.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.messenger.presentation.components.common.RoundedCard
import com.example.messenger.presentation.components.common.SectionHeader
import com.example.messenger.presentation.components.common.SegmentedToggle
import com.example.messenger.presentation.components.common.ToggleRow
import com.example.messenger.presentation.screens.ui.theme.AppBackground
import com.example.messenger.presentation.screens.ui.theme.AppBackgrounds
import com.example.messenger.presentation.screens.ui.theme.AppFont
import com.example.messenger.presentation.screens.ui.theme.AppTextSize
import com.example.messenger.presentation.screens.ui.theme.BarPosition
import com.example.messenger.presentation.screens.ui.theme.SavedWallpaper
import com.example.messenger.presentation.screens.ui.theme.ThemeMode
import com.example.messenger.presentation.screens.ui.theme.backgroundKeyFor
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import com.example.messenger.presentation.screens.ui.theme.wallpaperFileById
import com.example.messenger.presentation.viewmodel.AppearanceViewModel
import kotlin.math.roundToInt

@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    viewModel: AppearanceViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val background by viewModel.background.collectAsStateWithLifecycle()
    val intensity by viewModel.intensity.collectAsStateWithLifecycle()
    val barPosition by viewModel.barPosition.collectAsStateWithLifecycle()
    val textSize by viewModel.textSize.collectAsStateWithLifecycle()
    val appFont by viewModel.appFont.collectAsStateWithLifecycle()
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
    var renameTarget by remember { mutableStateOf<SavedWallpaper?>(null) }

    val pickWallpaper = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.importWallpaper(uri)
    }

    SettingsSubScaffold(title = "Appearance", onBack = onBack) {
        RoundedCard {
            ToggleRow(
                title = "Dark theme",
                checked = isDark,
                onCheckedChange = { viewModel.setDarkTheme(it) },
            )
        }

        SectionHeader(text = "Navigation bar")
        RoundedCard {
            LabeledSegmented(
                label = "Bar position",
                options = listOf("Top", "Bottom"),
                selectedIndex = if (barPosition == BarPosition.TOP) 0 else 1,
                onSelect = {
                    viewModel.setBarPosition(if (it == 0) BarPosition.TOP else BarPosition.BOTTOM)
                },
            )
        }

        SectionHeader(text = "Wallpaper")
        WallpaperGrid(
            presets = AppBackgrounds,
            saved = wallpapers,
            selectedKey = background,
            isDark = isDark,
            onSelectPreset = { viewModel.setBackground(it) },
            onSelectSaved = { viewModel.setBackground(backgroundKeyFor(it.id)) },
            onAdd = {
                pickWallpaper.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onRename = { renameTarget = it },
            onDelete = { viewModel.deleteWallpaper(it.id) },
        )

        SectionHeader(text = "Text")
        RoundedCard {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                LabeledSegmented(
                    label = "Text size",
                    options = AppTextSize.entries.map { it.label },
                    selectedIndex = AppTextSize.entries.indexOf(textSize),
                    onSelect = { viewModel.setTextSize(AppTextSize.entries[it]) },
                )
                LabeledSegmented(
                    label = "Font",
                    options = AppFont.entries.map { it.label },
                    selectedIndex = AppFont.entries.indexOf(appFont),
                    onSelect = { viewModel.setAppFont(AppFont.entries[it]) },
                )
            }
        }

        SectionHeader(text = "Intensity")
        IntensityCard(
            intensity = intensity,
            onPreview = { viewModel.previewIntensity(it) },
            onCommit = { viewModel.commitIntensity(it) },
        )
    }

    renameTarget?.let { target ->
        RenameWallpaperDialog(
            initial = target.name,
            onConfirm = { name ->
                viewModel.renameWallpaper(target.id, name)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }
}

@Composable
private fun LabeledSegmented(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val tokens = messengerTokens
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(text = label, color = tokens.textPrimary, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))
        SegmentedToggle(
            options = options,
            selectedIndex = selectedIndex.coerceAtLeast(0),
            onSelect = onSelect,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WallpaperGrid(
    presets: List<AppBackground>,
    saved: List<SavedWallpaper>,
    selectedKey: String,
    isDark: Boolean,
    onSelectPreset: (String) -> Unit,
    onSelectSaved: (SavedWallpaper) -> Unit,
    onAdd: () -> Unit,
    onRename: (SavedWallpaper) -> Unit,
    onDelete: (SavedWallpaper) -> Unit,
) {
    val tokens = messengerTokens
    val rows = (presets.map { GridTile.Preset(it) } +
        saved.map { GridTile.Saved(it) } +
        GridTile.Add).chunked(3)

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedWallpaperTile(
    wallpaper: SavedWallpaper,
    selected: Boolean,
    onApply: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = messengerTokens
    val context = LocalContext.current
    val file = remember(wallpaper.id) { wallpaperFileById(context, wallpaper.id) }
    var menuOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 2.5.dp,
                    color = if (selected) tokens.accent else tokens.panelBorder,
                    shape = RoundedCornerShape(16.dp),
                )
                .combinedClickable(
                    onClick = onApply,
                    onLongClick = { menuOpen = true },
                ),
        ) {
            AsyncImage(
                model = file,
                contentDescription = wallpaper.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(tokens.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = { menuOpen = false; onRename() },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = wallpaper.name, color = tokens.textPrimary, fontSize = 13.sp)
    }
}

@Composable
private fun RenameWallpaperDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename wallpaper") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun IntensityCard(
    intensity: Float,
    onPreview: (Float) -> Unit,
    onCommit: (Float) -> Unit,
) {
    val tokens = messengerTokens
    var dragging by remember { mutableStateOf(false) }
    var localValue by remember { mutableStateOf(intensity) }
    if (!dragging) localValue = intensity
    RoundedCard {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Background intensity",
                    color = tokens.textPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${(localValue * 100).roundToInt()}%",
                    color = tokens.textMuted,
                    fontSize = 13.sp,
                )
            }
            Slider(
                value = localValue,
                onValueChange = {
                    dragging = true
                    localValue = it
                    onPreview(it)
                },
                onValueChangeFinished = {
                    onCommit(localValue)
                    dragging = false
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = tokens.accent,
                    activeTrackColor = tokens.accent,
                    inactiveTrackColor = tokens.pillFill,
                ),
            )
            Text(
                text = "Lower values soften the wallpaper toward a flat background",
                color = tokens.textMuted,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun BackgroundTile(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
) {
    val tokens = messengerTokens
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 2.5.dp,
                    color = if (selected) tokens.accent else tokens.panelBorder,
                    shape = RoundedCornerShape(16.dp),
                )
                .clickable { onClick() },
        ) {
            preview()
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(tokens.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = tokens.textPrimary,
            fontSize = 13.sp,
        )
    }
}
