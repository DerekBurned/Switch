package com.example.messenger.presentation.effect

import com.example.messenger.domain.model.Conversation
import com.example.messenger.presentation.base.UiEffect

sealed interface SearchUsersEffect : UiEffect {
    data class ConversationCreated(val conversation: Conversation) : SearchUsersEffect
}
