package com.example.messenger.presentation.effect

import com.example.messenger.presentation.base.UiEffect

sealed interface ChangePhoneEffect : UiEffect {
    
    data object Done : ChangePhoneEffect
}
