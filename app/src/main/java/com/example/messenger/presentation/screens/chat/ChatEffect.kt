package com.example.messenger.presentation.screens.chat

import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.base.mvi.UiEffect

sealed interface ChatEffect : UiEffect {
    
    data class ShowError(val message: UiText) : ChatEffect
}
