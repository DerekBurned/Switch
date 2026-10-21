package com.example.messenger.presentation.screens.mediapicker

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.data.media.GalleryFilter
import com.example.messenger.data.media.GalleryItem
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.presentation.components.media.CameraCaptureScreen
import com.example.messenger.presentation.components.media.CameraPreviewCell
import com.example.messenger.presentation.featureflag.featureEnabled
import com.example.messenger.presentation.theme.LightGray
import com.example.messenger.presentation.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerBottomSheet(
    selected: Set<Uri>,
    onToggle: (Uri, String) -> Unit,
    onPreview: (List<GalleryItem>, Int) -> Unit,
    onCaptured: (Uri, String) -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
    cameraEnabled: Boolean,
    viewModel: MediaPickerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }

    var showCapture by remember { mutableStateOf(false) }
    val filtersEnabled = featureEnabled(FeatureFlag.MEDIA_PICKER_FILTERS)
    val cameraCellEnabled = featureEnabled(FeatureFlag.MEDIA_CAMERA)
    val viewerEnabled = featureEnabled(FeatureFlag.MEDIA_VIEWER)
    val activeFilter = if (filtersEnabled) state.activeFilter else GalleryFilter.ALL

    val gridItems = remember(state.captured, state.items, activeFilter) {
        (state.captured + state.items).filter { item ->
            when (activeFilter) {
                GalleryFilter.ALL -> true
                GalleryFilter.PHOTOS -> item.kind == MediaItem.IMAGE
                GalleryFilter.VIDEOS -> item.kind == MediaItem.VIDEO
            }
        }
    }

    if (showCapture && cameraCellEnabled) {
        CameraCaptureScreen(
            onResult = { uri, kind, durationMs ->
                viewModel.addCaptured(uri, kind, durationMs)
                onCaptured(uri, kind)
                showCapture = false
            },
            onDismiss = { showCapture = false },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (filtersEnabled) GalleryFilter.entries.forEach { filter ->
                        val isActive = state.activeFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isActive) PrimaryBlue else LightGray)
                                .clickable { viewModel.onFilterChange(filter) }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = filter.label(),
                                color = if (isActive) Color.White else Color.Gray,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
                if (selected.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(PrimaryBlue)
                            .clickable(onClick = onDone)
                            .padding(horizontal = 18.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = "Done (${selected.size})",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            HorizontalDivider(color = LightGray)

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                contentPadding = PaddingValues(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (cameraCellEnabled) {
                    item(key = "camera") {
                        CameraPreviewCell(
                            enabled = cameraEnabled,
                            onClick = { showCapture = true },
                            modifier = Modifier.aspectRatio(1f),
                        )
                    }
                }
                items(gridItems, key = { it.id }) { item ->
                    val index = gridItems.indexOf(item)
                    GalleryCell(
                        item = item,
                        isSelected = item.uri in selected,
                        onToggle = { onToggle(item.uri, item.kind) },
                        onOpen = {
                            if (viewerEnabled) onPreview(gridItems, index) else onToggle(item.uri, item.kind)
                        },
                    )
                }
            }
        }
    }
}


private fun GalleryFilter.label(): String = when (this) {
    GalleryFilter.ALL -> "All"
    GalleryFilter.PHOTOS -> "Photos"
    GalleryFilter.VIDEOS -> "Videos"
}
