package com.example.messenger.presentation.theme.model

import kotlinx.serialization.Serializable

/** A user-imported wallpaper the app has saved. [id] maps to a file, [name] is user-editable. */
@Serializable
data class SavedWallpaper(
    val id: String,
    val name: String,
)
