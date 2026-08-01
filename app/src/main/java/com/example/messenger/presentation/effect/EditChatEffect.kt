package com.example.messenger.presentation.effect

import com.example.messenger.presentation.base.UiEffect

sealed interface EditChatEffect : UiEffect {
    
    data object Done : EditChatEffect
}
