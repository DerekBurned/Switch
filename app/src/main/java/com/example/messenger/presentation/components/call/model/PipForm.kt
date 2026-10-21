package com.example.messenger.presentation.components.call.model

internal sealed interface PipForm {
    data class Video(val stream: VideoStream) : PipForm

    data object Bubble : PipForm

    data object Ended : PipForm
}
