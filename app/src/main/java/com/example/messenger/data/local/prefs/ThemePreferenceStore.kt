package com.example.messenger.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.messenger.presentation.theme.ThemeMode
import com.example.messenger.presentation.theme.model.AppFont
import com.example.messenger.presentation.theme.model.AppTextSize
import com.example.messenger.presentation.theme.model.BarPosition
import com.example.messenger.presentation.theme.model.SavedWallpaper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

@Singleton
class ThemePreferenceStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val backgroundKey = stringPreferencesKey("app_background")
    private val intensityKey = floatPreferencesKey("theme_intensity")
    private val barPositionKey = stringPreferencesKey("bar_position")
    private val textSizeKey = stringPreferencesKey("text_size")
    private val appFontKey = stringPreferencesKey("app_font")
    private val wallpapersKey = stringPreferencesKey("wallpapers_json")

    private val json = Json { ignoreUnknownKeys = true }

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        prefs[themeModeKey]
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    val background: Flow<String> = context.themeDataStore.data.map { prefs ->
        prefs[backgroundKey] ?: "neutral"
    }

    val barPosition: Flow<BarPosition> = context.themeDataStore.data.map { prefs ->
        prefs[barPositionKey]
            ?.let { runCatching { BarPosition.valueOf(it) }.getOrNull() }
            ?: BarPosition.TOP
    }

    val textSize: Flow<AppTextSize> = context.themeDataStore.data.map { prefs ->
        prefs[textSizeKey]
            ?.let { runCatching { AppTextSize.valueOf(it) }.getOrNull() }
            ?: AppTextSize.MEDIUM
    }

    val appFont: Flow<AppFont> = context.themeDataStore.data.map { prefs ->
        prefs[appFontKey]
            ?.let { runCatching { AppFont.valueOf(it) }.getOrNull() }
            ?: AppFont.DEFAULT
    }

    val wallpapers: Flow<List<SavedWallpaper>> = context.themeDataStore.data.map { prefs ->
        prefs[wallpapersKey]?.let { decodeWallpapers(it) } ?: emptyList()
    }

    private val liveIntensity = MutableStateFlow<Float?>(null)

    val intensity: Flow<Float> = combine(
        context.themeDataStore.data.map { prefs -> prefs[intensityKey] ?: 1f },
        liveIntensity,
    ) { stored, live -> live ?: stored }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs -> prefs[themeModeKey] = mode.name }
    }

    suspend fun setBackground(key: String) {
        context.themeDataStore.edit { prefs -> prefs[backgroundKey] = key }
    }

    fun previewIntensity(value: Float) {
        liveIntensity.value = value.coerceIn(0f, 1f)
    }

    suspend fun setIntensity(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        liveIntensity.value = clamped
        context.themeDataStore.edit { prefs -> prefs[intensityKey] = clamped }
    }

    suspend fun setBarPosition(position: BarPosition) {
        context.themeDataStore.edit { prefs -> prefs[barPositionKey] = position.name }
    }

    suspend fun setTextSize(size: AppTextSize) {
        context.themeDataStore.edit { prefs -> prefs[textSizeKey] = size.name }
    }

    suspend fun setAppFont(font: AppFont) {
        context.themeDataStore.edit { prefs -> prefs[appFontKey] = font.name }
    }

    suspend fun addWallpaper(wallpaper: SavedWallpaper) {
        context.themeDataStore.edit { prefs ->
            val current = prefs[wallpapersKey]?.let { decodeWallpapers(it) } ?: emptyList()
            prefs[wallpapersKey] = json.encodeToString(current + wallpaper)
        }
    }

    suspend fun renameWallpaper(id: String, name: String) {
        context.themeDataStore.edit { prefs ->
            val current = prefs[wallpapersKey]?.let { decodeWallpapers(it) } ?: emptyList()
            prefs[wallpapersKey] = json.encodeToString(
                current.map { if (it.id == id) it.copy(name = name) else it },
            )
        }
    }

    suspend fun removeWallpaper(id: String) {
        context.themeDataStore.edit { prefs ->
            val current = prefs[wallpapersKey]?.let { decodeWallpapers(it) } ?: emptyList()
            prefs[wallpapersKey] = json.encodeToString(current.filterNot { it.id == id })
        }
    }

    private fun decodeWallpapers(raw: String): List<SavedWallpaper> =
        runCatching { json.decodeFromString<List<SavedWallpaper>>(raw) }.getOrDefault(emptyList())
}
