package com.example.messenger.presentation.components.media.model

import android.net.Uri

data class MediaSource(
    val key: String,
    val isVideo: Boolean,
    val model: Any,
    val playUri: Uri,
    val blurHash: String? = null,
)
