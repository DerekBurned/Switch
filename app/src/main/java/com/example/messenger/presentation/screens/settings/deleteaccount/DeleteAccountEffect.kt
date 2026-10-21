package com.example.messenger.presentation.screens.settings.deleteaccount

import com.example.messenger.presentation.base.mvi.UiEffect

sealed interface DeleteAccountEffect : UiEffect {

    data object Deleted : DeleteAccountEffect
}
