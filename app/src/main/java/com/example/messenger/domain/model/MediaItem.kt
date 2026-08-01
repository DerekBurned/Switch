package com.example.messenger.domain.model



data class MediaItem(
    val id: String = "",
    val kind: String = IMAGE,
    val url: String = "",
    val storagePath: String = "",
    val blurHash: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val fileKey: String? = null,
) {
    companion object {
        const val IMAGE = "IMAGE"
        const val VIDEO = "VIDEO"
    }
}
