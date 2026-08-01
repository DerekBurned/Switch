package com.example.messenger.data.service

import com.example.messenger.data.remote.firebase.RealtimeDbService
import com.example.messenger.domain.model.PresenceState
import com.example.messenger.domain.model.UserPresence
import com.example.messenger.domain.service.IPresenceService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebasePresenceService @Inject constructor(
    private val realtimeDbService: RealtimeDbService,
    private val firebaseAuth: FirebaseAuth
) : IPresenceService {

    override fun observePresence(userId: String): Flow<UserPresence> {
        return realtimeDbService.observePresence(userId).map { data ->
            UserPresence(
                state = PresenceState.fromString(data["state"] as? String),
                lastSeen = (data["lastSeen"] as? Number)?.toLong() ?: 0L
            )
        }
    }

    override fun observeMultiplePresence(userIds: List<String>): Flow<Map<String, UserPresence>> {
        if (userIds.isEmpty()) return flowOf(emptyMap())

        val flows = userIds.map { userId ->
            observePresence(userId).map { presence -> userId to presence }
        }

        return combine(flows) { results ->
            results.toMap()
        }
    }

    override suspend fun setMyPresence(state: PresenceState): Boolean {
        val userId = firebaseAuth.currentUser?.uid ?: return false
        realtimeDbService.setPresence(
            userId = userId,
            state = state.name.lowercase(),
            lastSeen = System.currentTimeMillis()
        )
        return true
    }

    override suspend fun setupOnDisconnect() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        realtimeDbService.setupOnDisconnect(userId)
    }

    override suspend fun removePresence() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        realtimeDbService.cancelOnDisconnect(userId)
        realtimeDbService.removePresence(userId)
    }
}
