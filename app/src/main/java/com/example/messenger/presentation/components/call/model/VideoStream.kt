package com.example.messenger.presentation.components.call.model

sealed interface VideoStream {
    data object Local : VideoStream

    data class Remote(val uid: Int) : VideoStream
}
