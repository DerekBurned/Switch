package com.example.messenger.presentation.effect

import com.example.messenger.presentation.base.UiEffect
import com.example.messenger.presentation.base.UiText

sealed interface ChatEffect : UiEffect {
    
    data class ShowError(val message: UiText) : ChatEffect
}
