package com.example.messenger.presentation.screens.chats

import androidx.compose.runtime.Immutable
import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.model.User
import com.example.messenger.domain.model.presence.UserPresence
import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.base.mvi.UiState

@Immutable
data class ConversationsUiState(
    val isLoading: Boolean = false,
    val conversations: List<Conversation> = emptyList(),
    val error: UiText? = null,
    val presenceMap: Map<String, UserPresence> = emptyMap(),
    val aliases: Map<String, String> = emptyMap(),
    val avatars: Map<String, String?> = emptyMap(),
    val participants: Map<String, User> = emptyMap(),
    val isRefreshing: Boolean = false,
    val currentUserId: String = "",
) : UiState
