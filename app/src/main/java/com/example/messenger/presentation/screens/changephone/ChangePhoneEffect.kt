package com.example.messenger.presentation.screens.changephone

import com.example.messenger.presentation.base.mvi.UiEffect

sealed interface ChangePhoneEffect : UiEffect {
    
    data object Done : ChangePhoneEffect
}
