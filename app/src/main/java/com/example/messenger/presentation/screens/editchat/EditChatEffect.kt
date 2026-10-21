package com.example.messenger.presentation.screens.editchat

import com.example.messenger.presentation.base.mvi.UiEffect

sealed interface EditChatEffect : UiEffect {
    
    data object Done : EditChatEffect
}
