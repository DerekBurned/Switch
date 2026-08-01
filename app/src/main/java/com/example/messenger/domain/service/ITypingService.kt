package com.example.messenger.domain.service

import kotlinx.coroutines.flow.Flow

interface ITypingService {
    fun observeTyping(conversationId: String): Flow<Set<String>>
    suspend fun setTyping(conversationId: String)
    suspend fun clearTyping(conversationId: String)
    fun clearTypingNow(conversationId: String)
}
