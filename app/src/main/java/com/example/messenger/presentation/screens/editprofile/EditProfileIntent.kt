package com.example.messenger.presentation.screens.editprofile

import com.example.messenger.presentation.base.mvi.UiIntent

sealed interface EditProfileIntent : UiIntent {
    data class NameChange(val value: String) : EditProfileIntent
    data class PhoneChange(val value: String) : EditProfileIntent
    data class UsernameChange(val value: String) : EditProfileIntent
    data class DobChange(val value: String) : EditProfileIntent
    data object Save : EditProfileIntent
    data object ClearError : EditProfileIntent
}
