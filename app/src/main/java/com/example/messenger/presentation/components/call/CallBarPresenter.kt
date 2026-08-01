package com.example.messenger.presentation.components.call

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CallBarMode { BAR, HIDDEN, BUBBLE }

object CallBarPresenter {

    private val _mode = MutableStateFlow(CallBarMode.BAR)
    val mode: StateFlow<CallBarMode> = _mode.asStateFlow()

    @Volatile
    var bubbleOffset: Offset? = null

    fun showBar() {
        _mode.value = CallBarMode.BAR
    }

    fun hide() {
        _mode.value = CallBarMode.HIDDEN
    }

    fun minimizeToBubble() {
        _mode.value = CallBarMode.BUBBLE
    }

    fun reset() {
        _mode.value = CallBarMode.BAR
        bubbleOffset = null
    }
}
