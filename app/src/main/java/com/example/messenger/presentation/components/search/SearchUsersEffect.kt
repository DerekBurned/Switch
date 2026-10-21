package com.example.messenger.presentation.components.search

import com.example.messenger.domain.model.Conversation
import com.example.messenger.presentation.base.mvi.UiEffect

sealed interface SearchUsersEffect : UiEffect {
    data class ConversationCreated(val conversation: Conversation) : SearchUsersEffect
}
