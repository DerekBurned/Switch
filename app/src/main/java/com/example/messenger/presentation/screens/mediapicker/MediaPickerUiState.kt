package com.example.messenger.presentation.screens.mediapicker

import com.example.messenger.data.media.GalleryFilter
import com.example.messenger.data.media.GalleryItem
import com.example.messenger.presentation.base.mvi.UiState

data class MediaPickerUiState(
    val activeFilter: GalleryFilter = GalleryFilter.ALL,
    val items: List<GalleryItem> = emptyList(),
    val captured: List<GalleryItem> = emptyList(),
    val isLoading: Boolean = false,
) : UiState
