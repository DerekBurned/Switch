package com.example.messenger.presentation.state

import android.net.Uri
import com.example.messenger.presentation.base.UiState

enum class GalleryFilter { ALL, PHOTOS, VIDEOS }

data class GalleryItem(
    val id: Long,
    val uri: Uri,
    val kind: String,
    val durationMs: Long,
    val dateAdded: Long,
)

data class MediaPickerUiState(
    val activeFilter: GalleryFilter = GalleryFilter.ALL,
    val items: List<GalleryItem> = emptyList(),
    val captured: List<GalleryItem> = emptyList(),
    val isLoading: Boolean = false,
) : UiState
