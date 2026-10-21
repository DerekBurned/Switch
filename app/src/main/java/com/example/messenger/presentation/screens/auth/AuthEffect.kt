package com.example.messenger.presentation.screens.auth

import com.example.messenger.presentation.base.mvi.UiEffect

sealed interface AuthEffect : UiEffect {
    
    data object AuthSucceeded : AuthEffect

    data object PhoneLinked : AuthEffect
}
