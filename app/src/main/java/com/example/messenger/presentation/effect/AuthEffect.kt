package com.example.messenger.presentation.effect

import com.example.messenger.presentation.base.UiEffect

sealed interface AuthEffect : UiEffect {
    
    data object AuthSucceeded : AuthEffect

    data object PhoneLinked : AuthEffect
}
