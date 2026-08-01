package com.example.messenger.presentation.state

import com.example.messenger.domain.model.User
import com.example.messenger.presentation.base.UiState
import com.example.messenger.presentation.base.UiText

enum class MediaTab { PHOTO, VIDEO, FILE }

data class ChatUserProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val displayName: String = "",
    val isOnline: Boolean = false,
    val lastSeen: String = "",
    val mediaTab: MediaTab = MediaTab.PHOTO,
    val mediaItems: List<String> = emptyList(),
    val avatarPhotos: List<String> = emptyList(),
    val seededAvatarUrl: String? = null,
    val error: UiText? = null,
) : UiState
