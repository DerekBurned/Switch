package com.example.messenger.data.media

import android.net.Uri

data class GalleryItem(
    val id: Long,
    val uri: Uri,
    val kind: String,
    val durationMs: Long,
    val dateAdded: Long,
)
