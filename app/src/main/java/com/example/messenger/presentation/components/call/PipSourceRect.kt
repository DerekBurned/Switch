package com.example.messenger.presentation.components.call

import android.graphics.Rect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PipSourceRect {

    private val _rect = MutableStateFlow<Rect?>(null)
    val rect: StateFlow<Rect?> = _rect.asStateFlow()

    fun set(value: Rect?) {
        if (_rect.value != value) _rect.value = value
    }

    fun clear() {
        _rect.value = null
    }
}
