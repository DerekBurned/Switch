package com.example.messenger.domain.repository

import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface IConversationRepository {
    fun getAllConversations(): Flow<List<Conversation>>
    suspend fun getConversationById(conversationId: String): Result<Conversation?>
    suspend fun getConversationsForProfile(profile: Profile): Result<List<Conversation>>
    suspend fun createConversation(participantIds: List<String>): Result<Conversation>
    suspend fun deleteConversation(conversationId: String): Result<Unit>
    suspend fun deleteConversationForEveryone(conversationId: String): Result<Unit>
    suspend fun clearConversationForMe(conversationId: String): Result<Unit>
    suspend fun markConversationAsRead(conversationId: String): Result<Unit>
    suspend fun markConversationAsUnread(conversationId: String): Result<Unit>
    suspend fun observeRemoteConversations(): Flow<List<Conversation>>
    suspend fun syncConversations()

}