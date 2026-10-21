package com.example.messenger.presentation.components.chat.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class MessageContextMenuState {
    var active by mutableStateOf<MessageContextMenuItem?>(null)
        private set
    internal var renderingKey by mutableStateOf<Any?>(null)

    fun open(item: MessageContextMenuItem) {
        active = item
    }

    fun dismiss() {
        active = null
    }
}
