package com.example.messenger.testutil.fakes

import com.example.messenger.domain.service.ITypingService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeTypingService : ITypingService {

    val typing = MutableStateFlow<Set<String>>(emptySet())
    var setCount = 0
    var clearCount = 0

    override fun observeTyping(conversationId: String): Flow<Set<String>> = typing

    override suspend fun setTyping(conversationId: String) {
        setCount++
    }

    override suspend fun clearTyping(conversationId: String) {
        clearCount++
    }

    override fun clearTypingNow(conversationId: String) = Unit
}
