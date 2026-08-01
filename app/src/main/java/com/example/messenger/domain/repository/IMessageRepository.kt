package com.example.messenger.domain.repository

import com.example.messenger.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface IMessageRepository {
    fun getMessagesStream(conversationId: String): Flow<List<Message>>
    suspend fun sendMessage(message: Message): Result<Unit>
    suspend fun persistIncomingMessage(message: Message)
    suspend fun deleteMessageForEveryone(message: Message): Result<Unit>
    suspend fun deleteMessageForMe(message: Message): Result<Unit>
    suspend fun markMessageAsRead(message: Message): Result<Unit>

    suspend fun observeRecentMessages(conversationId: String, limit: Long = 100): Flow<Result<Unit>>

    suspend fun loadOlderMessages(conversationId: String, limit: Long = 100): Result<Int>

    suspend fun backfillRecentMessages(conversationId: String, limit: Long = 30): Result<Int>
}