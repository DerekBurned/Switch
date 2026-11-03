package com.example.messenger.presentation.screens.chatuserprofile

import com.example.messenger.domain.model.MediaTransfer
import com.example.messenger.domain.model.User
import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.base.mvi.UiState

data class ChatUserProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val partnerId: String = "",
    val displayName: String = "",
    val isOnline: Boolean = false,
    val lastSeen: String = "",
    val mediaTab: MediaTab = MediaTab.PHOTO,
    val sharedMedia: SharedMedia = SharedMedia(),
    val mediaLoading: Boolean = false,
    val mediaEndReached: Boolean = false,
    val mediaPagesLoaded: Int = 0,
    val transfers: Map<String, MediaTransfer> = emptyMap(),
    val avatarPhotos: List<String> = emptyList(),
    val seededAvatarUrl: String? = null,
    val error: UiText? = null,
) : UiState
