package com.example.messenger.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.messenger.data.media.MediaStoreGallery
import com.example.messenger.presentation.state.GalleryFilter
import com.example.messenger.presentation.state.GalleryItem
import com.example.messenger.presentation.state.MediaPickerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaPickerViewModel @Inject constructor(
    private val gallery: MediaStoreGallery,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaPickerUiState())
    val uiState: StateFlow<MediaPickerUiState> = _uiState.asStateFlow()

    fun loadIfNeeded() {
        if (_uiState.value.items.isEmpty()) load(_uiState.value.activeFilter)
    }

    fun onFilterChange(filter: GalleryFilter) {
        if (_uiState.value.activeFilter == filter) return
        load(filter)
    }

    fun addCaptured(uri: Uri, kind: String, durationMs: Long) {
        val item = GalleryItem(
            id = -System.nanoTime(),
            uri = uri,
            kind = kind,
            durationMs = durationMs,
            dateAdded = System.currentTimeMillis() / 1000,
        )
        _uiState.update { it.copy(captured = listOf(item) + it.captured) }
        Log.d(TAG, "addCaptured kind=$kind uri=$uri")
    }

    private fun load(filter: GalleryFilter) {
        viewModelScope.launch {
            _uiState.update { it.copy(activeFilter = filter, isLoading = true) }
            val items = gallery.load(filter)
            _uiState.update { it.copy(items = items, isLoading = false) }
            Log.d(TAG, "loaded ${items.size} items for $filter")
        }
    }

    private companion object {
        const val TAG = "MediaPickerViewModel"
    }
}
