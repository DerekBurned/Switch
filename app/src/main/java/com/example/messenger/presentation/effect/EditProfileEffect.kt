package com.example.messenger.presentation.effect

import com.example.messenger.presentation.base.UiEffect

sealed interface EditProfileEffect : UiEffect {
    data object Saved : EditProfileEffect
}
