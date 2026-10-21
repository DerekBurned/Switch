package com.example.messenger.testutil.fakes

import com.example.messenger.domain.model.Message
import com.example.messenger.domain.repository.IMessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class FakeMessageRepository : IMessageRepository {

    val messages = MutableStateFlow<List<Message>>(emptyList())
    var messagesError: Throwable? = null
    val sent = mutableListOf<Message>()
    var sendResult: Result<Unit> = Result.success(Unit)
    val markedRead = mutableListOf<Message>()
    var markReadResult: Result<Unit> = Result.success(Unit)
    var recentResults: Flow<Result<Unit>> = flowOf(Result.success(Unit))
    var loadOlderResult: Result<Int> = Result.success(0)
    val backfilled = mutableListOf<Pair<String, Long>>()
    var backfillResult: (String) -> Result<Int> = { Result.success(1) }
    val deletedForEveryone = mutableListOf<Message>()
    val deletedForMe = mutableListOf<Message>()
    var deleteResult: Result<Unit> = Result.success(Unit)

    override fun getMessagesStream(conversationId: String): Flow<List<Message>> =
        messagesError?.let { error -> flow { throw error } } ?: messages

    override suspend fun sendMessage(message: Message): Result<Unit> {
        sent += message
        return sendResult
    }

    override suspend fun persistIncomingMessage(message: Message) = Unit

    override suspend fun deleteMessageForEveryone(message: Message): Result<Unit> {
        deletedForEveryone += message
        return deleteResult
    }

    override suspend fun deleteMessageForMe(message: Message): Result<Unit> {
        deletedForMe += message
        return deleteResult
    }

    override suspend fun markMessageAsRead(message: Message): Result<Unit> {
        markedRead += message
        return markReadResult
    }

    override suspend fun observeRecentMessages(
        conversationId: String,
        limit: Long,
    ): Flow<Result<Unit>> = recentResults

    override suspend fun loadOlderMessages(conversationId: String, limit: Long): Result<Int> =
        loadOlderResult

    override suspend fun backfillRecentMessages(conversationId: String, limit: Long): Result<Int> {
        backfilled += conversationId to limit
        return backfillResult(conversationId)
    }
}
