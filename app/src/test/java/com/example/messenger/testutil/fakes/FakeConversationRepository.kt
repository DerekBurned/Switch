package com.example.messenger.testutil.fakes

import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.model.Profile
import com.example.messenger.domain.repository.IConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

class FakeConversationRepository : IConversationRepository {

    val conversations = MutableStateFlow<List<Conversation>>(emptyList())
    var conversationsError: Throwable? = null
    var createdConversation: Conversation = Conversation(id = "conv1")
    val createdParticipants = mutableListOf<List<String>>()
    var syncCount = 0
    var syncError: Throwable? = null

    var failWith: Throwable? = null

    private fun <T> result(value: T): Result<T> =
        failWith?.let { Result.failure(it) } ?: Result.success(value)

    override fun getAllConversations(): Flow<List<Conversation>> =
        conversationsError?.let { error -> flow { throw error } } ?: conversations

    override suspend fun getConversationById(conversationId: String): Result<Conversation?> =
        result(conversations.value.find { it.id == conversationId })

    override suspend fun getConversationsForProfile(profile: Profile): Result<List<Conversation>> =
        result(conversations.value)

    override suspend fun createConversation(participantIds: List<String>): Result<Conversation> {
        createdParticipants += participantIds
        return result(createdConversation)
    }

    val deletedIds = mutableListOf<String>()

    override suspend fun deleteConversation(conversationId: String): Result<Unit> {
        deletedIds += conversationId
        return result(Unit)
    }

    override suspend fun deleteConversationForEveryone(conversationId: String): Result<Unit> = result(Unit)

    override suspend fun clearConversationForMe(conversationId: String): Result<Unit> = result(Unit)

    override suspend fun markConversationAsRead(conversationId: String): Result<Unit> = result(Unit)

    override suspend fun markConversationAsUnread(conversationId: String): Result<Unit> = result(Unit)

    override suspend fun observeRemoteConversations(): Flow<List<Conversation>> = conversations

    override suspend fun syncConversations() {
        syncCount++
        syncError?.let { throw it }
    }
}
