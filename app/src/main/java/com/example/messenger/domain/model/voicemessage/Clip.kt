package com.example.messenger.domain.model.voicemessage

import androidx.compose.runtime.Immutable

@Immutable
sealed class Clip {
    abstract val url: String
    abstract val durationMs: Long
    abstract val sizeBytes: Long
    abstract val fileKey: String?

    abstract val fileExtension: String

    abstract val previewLabel: String

    abstract val maxDurationMs: Long?

    val exceedsDurationLimit: Boolean
        get() = maxDurationMs?.let { durationMs > it } == true

    abstract fun withUpload(url: String, fileKey: String?): Clip

    data class Voice(
        override val url: String = "",
        override val durationMs: Long = 0L,
        override val sizeBytes: Long = 0L,
        override val fileKey: String? = null,
        val waveform: List<Int> = emptyList(),
    ) : Clip() {
        override val fileExtension: String get() = FILE_EXTENSION

        override val previewLabel: String get() = PREVIEW_LABEL

        override val maxDurationMs: Long get() = MAX_DURATION_MS

        override fun withUpload(url: String, fileKey: String?): Clip =
            copy(url = url, fileKey = fileKey)

        companion object {
            const val MAX_DURATION_MS = 60 * 60 * 1000L
            const val PREVIEW_LABEL = "Voice message"
            const val FILE_EXTENSION = "m4a"
        }
    }

    data class Video(
        override val url: String = "",
        override val durationMs: Long = 0L,
        override val sizeBytes: Long = 0L,
        override val fileKey: String? = null,
    ) : Clip() {
        override val fileExtension: String get() = FILE_EXTENSION

        override val previewLabel: String get() = PREVIEW_LABEL

        override val maxDurationMs: Long get() = MAX_DURATION_MS

        override fun withUpload(url: String, fileKey: String?): Clip =
            copy(url = url, fileKey = fileKey)

        companion object {
            const val MAX_DURATION_MS = 2 * 60 * 1000L
            const val PREVIEW_LABEL = "Video message"
            const val FILE_EXTENSION = "mp4"
        }
    }
}
