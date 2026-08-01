package com.example.messenger.data.service

import com.example.messenger.data.remote.firebase.RealtimeDbService
import com.example.messenger.domain.service.ITypingService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseTypingService @Inject constructor(
    private val realtimeDbService: RealtimeDbService,
    private val firebaseAuth: FirebaseAuth
) : ITypingService {

    override fun observeTyping(conversationId: String): Flow<Set<String>> {
        return realtimeDbService.observeTyping(conversationId).map { data ->
            val currentUserId = firebaseAuth.currentUser?.uid
            val now = System.currentTimeMillis()
            data.entries
                .filter { it.key != currentUserId && isFresh(it.value, now) }
                .map { it.key }
                .toSet()
        }
    }

    override suspend fun setTyping(conversationId: String) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        realtimeDbService.setTyping(conversationId, userId)
    }

    override suspend fun clearTyping(conversationId: String) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        realtimeDbService.clearTyping(conversationId, userId)
    }

    override fun clearTypingNow(conversationId: String) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        realtimeDbService.clearTypingNow(conversationId, userId)
    }

    private fun isFresh(value: Any?, now: Long): Boolean {
        val timestamp = (value as? Map<*, *>)?.get("timestamp") as? Number ?: return true
        return now - timestamp.toLong() <= TYPING_STALE_MS
    }

    private companion object {
        const val TYPING_STALE_MS = 6_000L
    }
}
