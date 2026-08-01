package com.example.messenger.presentation.effect

import com.example.messenger.presentation.base.UiEffect

sealed interface EditContactDataEffect : UiEffect {
    data object Saved : EditContactDataEffect
    data object Deleted : EditContactDataEffect
}
