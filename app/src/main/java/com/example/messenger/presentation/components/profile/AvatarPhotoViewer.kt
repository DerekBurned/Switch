package com.example.messenger.presentation.components.profile

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.messenger.presentation.components.media.FullscreenMediaPager
import com.example.messenger.presentation.components.media.MediaSource

@Composable
fun AvatarPhotoViewer(
    photos: List<String>,
    onDismiss: () -> Unit,
    onAddClick: (() -> Unit)? = null,
) {
    if (photos.isEmpty()) return
    val sources = remember(photos) {
        photos.map { url ->
            MediaSource(key = url, isVideo = false, model = url, playUri = Uri.parse(url))
        }
    }
    FullscreenMediaPager(
        sources = sources,
        startIndex = 0,
        onDismiss = onDismiss,
        onAddClick = onAddClick,
    )
}
