package com.example.messenger.presentation.screens.settings.appearance

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.messenger.data.local.prefs.ThemePreferenceStore
import com.example.messenger.presentation.theme.ThemeMode
import com.example.messenger.presentation.theme.backgroundKeyFor
import com.example.messenger.presentation.theme.model.AppFont
import com.example.messenger.presentation.theme.model.AppTextSize
import com.example.messenger.presentation.theme.model.BarPosition
import com.example.messenger.presentation.theme.model.SavedWallpaper
import com.example.messenger.presentation.theme.wallpaperFileById
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val themePreferenceStore: ThemePreferenceStore,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themePreferenceStore.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val background: StateFlow<String> = themePreferenceStore.background
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "neutral")

    val intensity: StateFlow<Float> = themePreferenceStore.intensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1f)

    val barPosition: StateFlow<BarPosition> = themePreferenceStore.barPosition
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BarPosition.TOP)

    val textSize: StateFlow<AppTextSize> = themePreferenceStore.textSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppTextSize.MEDIUM)

    val appFont: StateFlow<AppFont> = themePreferenceStore.appFont
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppFont.DEFAULT)

    val wallpapers: StateFlow<List<SavedWallpaper>> = themePreferenceStore.wallpapers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            themePreferenceStore.setThemeMode(if (enabled) ThemeMode.DARK else ThemeMode.LIGHT)
        }
    }

    fun setBackground(key: String) {
        viewModelScope.launch { themePreferenceStore.setBackground(key) }
    }

    fun previewIntensity(value: Float) {
        themePreferenceStore.previewIntensity(value)
    }

    fun commitIntensity(value: Float) {
        viewModelScope.launch { themePreferenceStore.setIntensity(value) }
    }

    fun setBarPosition(position: BarPosition) {
        viewModelScope.launch { themePreferenceStore.setBarPosition(position) }
    }

    fun setTextSize(size: AppTextSize) {
        viewModelScope.launch { themePreferenceStore.setTextSize(size) }
    }

    fun setAppFont(font: AppFont) {
        viewModelScope.launch { themePreferenceStore.setAppFont(font) }
    }

    fun importWallpaper(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val id = System.currentTimeMillis().toString()
                val file = wallpaperFileById(context, id).apply { parentFile?.mkdirs() }
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                } ?: return@runCatching
                val name = "Wallpaper ${wallpapers.value.size + 1}"
                themePreferenceStore.addWallpaper(SavedWallpaper(id, name))
                themePreferenceStore.setBackground(backgroundKeyFor(id))
            }
        }
    }

    fun renameWallpaper(id: String, name: String) {
        val trimmed = name.trim().ifBlank { return }
        viewModelScope.launch { themePreferenceStore.renameWallpaper(id, trimmed) }
    }

    fun deleteWallpaper(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (background.value == backgroundKeyFor(id)) {
                themePreferenceStore.setBackground("neutral")
            }
            themePreferenceStore.removeWallpaper(id)
            runCatching { wallpaperFileById(context, id).delete() }
        }
    }
}
