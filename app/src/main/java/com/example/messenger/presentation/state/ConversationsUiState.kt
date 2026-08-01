package com.example.messenger.presentation.state

import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.model.UserPresence
import androidx.compose.runtime.Immutable
import com.example.messenger.presentation.base.UiState
import com.example.messenger.presentation.base.UiText

@Immutable
data class ConversationsUiState(
    val isLoading: Boolean = false,
    val conversations: List<Conversation> = emptyList(),
    val error: UiText? = null,
    val presenceMap: Map<String, UserPresence> = emptyMap(),
    val aliases: Map<String, String> = emptyMap(),
    val avatars: Map<String, String?> = emptyMap(),
    val isRefreshing: Boolean = false,
    val currentUserId: String = "",
) : UiState
