package com.example.messenger.presentation.screens.settings.appearance

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.presentation.components.common.RoundedCard
import com.example.messenger.presentation.components.common.SectionHeader
import com.example.messenger.presentation.components.common.ToggleRow
import com.example.messenger.presentation.featureflag.featureEnabled
import com.example.messenger.presentation.screens.settings.SettingsSubScaffold
import com.example.messenger.presentation.theme.AppBackgrounds
import com.example.messenger.presentation.theme.ThemeMode
import com.example.messenger.presentation.theme.backgroundKeyFor
import com.example.messenger.presentation.theme.model.AppFont
import com.example.messenger.presentation.theme.model.AppTextSize
import com.example.messenger.presentation.theme.model.BarPosition
import com.example.messenger.presentation.theme.model.SavedWallpaper

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

    val textSizeEnabled = featureEnabled(FeatureFlag.APPEARANCE_TEXT_SIZE)
    val fontEnabled = featureEnabled(FeatureFlag.APPEARANCE_FONT)
    SettingsSubScaffold(title = "Appearance", onBack = onBack) {
        if (featureEnabled(FeatureFlag.APPEARANCE_DARK_THEME)) {
            RoundedCard {
                ToggleRow(
                    title = "Dark theme",
                    checked = isDark,
                    onCheckedChange = { viewModel.setDarkTheme(it) },
                )
            }
        }

        if (featureEnabled(FeatureFlag.APPEARANCE_BAR_POSITION)) {
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
        }

        if (featureEnabled(FeatureFlag.APPEARANCE_WALLPAPERS)) {
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
                customEnabled = featureEnabled(FeatureFlag.APPEARANCE_CUSTOM_WALLPAPER),
            )
        }

        if (textSizeEnabled || fontEnabled) {
            SectionHeader(text = "Text")
            RoundedCard {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    if (textSizeEnabled) {
                        LabeledSegmented(
                            label = "Text size",
                            options = AppTextSize.entries.map { it.label },
                            selectedIndex = AppTextSize.entries.indexOf(textSize),
                            onSelect = { viewModel.setTextSize(AppTextSize.entries[it]) },
                        )
                    }
                    if (fontEnabled) {
                        LabeledSegmented(
                            label = "Font",
                            options = AppFont.entries.map { it.label },
                            selectedIndex = AppFont.entries.indexOf(appFont),
                            onSelect = { viewModel.setAppFont(AppFont.entries[it]) },
                        )
                    }
                }
            }
        }

        if (featureEnabled(FeatureFlag.APPEARANCE_WALLPAPER_INTENSITY)) {
            SectionHeader(text = "Intensity")
            IntensityCard(
                intensity = intensity,
                onPreview = { viewModel.previewIntensity(it) },
                onCommit = { viewModel.commitIntensity(it) },
            )
        }
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
