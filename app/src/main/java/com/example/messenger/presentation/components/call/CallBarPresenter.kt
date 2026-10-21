package com.example.messenger.presentation.components.call

import androidx.compose.ui.geometry.Offset
import com.example.messenger.presentation.components.call.model.CallBarMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CallBarPresenter {

    private val _mode = MutableStateFlow(CallBarMode.BAR)
    val mode: StateFlow<CallBarMode> = _mode.asStateFlow()

    @Volatile
    var bubbleOffset: Offset? = null

    @Volatile
    private var restoreMode: CallBarMode = CallBarMode.BAR

    @Volatile
    var videoDismissed: Boolean = false
        private set

    @Volatile
    var floatingEnabled: Boolean = true
        private set

    fun setFloatingEnabled(enabled: Boolean) {
        floatingEnabled = enabled
        if (!enabled && _mode.value.isFloating()) _mode.value = CallBarMode.BAR
    }

    fun showBar() {
        _mode.value = CallBarMode.BAR
    }

    fun minimizeToBubble() {
        if (!floatingEnabled) return
        _mode.value = CallBarMode.BUBBLE
    }

    fun showVideoTile() {
        if (videoDismissed || !floatingEnabled) return
        _mode.value = CallBarMode.VIDEO
    }

    fun collapseVideoTile() {
        if (_mode.value == CallBarMode.VIDEO) _mode.value = CallBarMode.BUBBLE
    }

    fun hide() {
        val current = _mode.value
        if (current == CallBarMode.VIDEO) videoDismissed = true
        restoreMode = if (current == CallBarMode.HIDDEN) CallBarMode.BAR else current
        _mode.value = CallBarMode.HIDDEN
    }

    fun restore() {
        if (restoreMode == CallBarMode.VIDEO) videoDismissed = false
        _mode.value = if (!floatingEnabled && restoreMode.isFloating()) CallBarMode.BAR else restoreMode
    }

    private fun CallBarMode.isFloating(): Boolean =
        this == CallBarMode.BUBBLE || this == CallBarMode.VIDEO

    fun reset() {
        _mode.value = CallBarMode.BAR
        bubbleOffset = null
        restoreMode = CallBarMode.BAR
        videoDismissed = false
    }
}
