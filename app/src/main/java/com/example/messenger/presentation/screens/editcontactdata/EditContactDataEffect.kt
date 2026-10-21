package com.example.messenger.presentation.screens.editcontactdata

import com.example.messenger.presentation.base.mvi.UiEffect

sealed interface EditContactDataEffect : UiEffect {
    data object Saved : EditContactDataEffect
    data object Deleted : EditContactDataEffect
}
