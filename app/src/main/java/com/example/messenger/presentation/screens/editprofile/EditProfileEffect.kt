package com.example.messenger.presentation.screens.editprofile

import com.example.messenger.presentation.base.mvi.UiEffect

sealed interface EditProfileEffect : UiEffect {
    data object Saved : EditProfileEffect
}
