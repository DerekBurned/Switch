package com.example.messenger.domain.model

sealed interface MediaTransfer {
    val progress: Float

    data class Uploading(override val progress: Float) : MediaTransfer
    data class Downloading(override val progress: Float) : MediaTransfer

    data object Completed : MediaTransfer {
        override val progress: Float = 1f
    }

    data object Failed : MediaTransfer {
        override val progress: Float = 0f
    }

    data object Cancelled : MediaTransfer {
        override val progress: Float = 0f
    }
}
