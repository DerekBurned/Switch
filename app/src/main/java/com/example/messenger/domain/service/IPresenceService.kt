package com.example.messenger.domain.service

import com.example.messenger.domain.model.presence.PresenceState
import com.example.messenger.domain.model.presence.UserPresence
import kotlinx.coroutines.flow.Flow

interface IPresenceService {
    fun observePresence(userId: String): Flow<UserPresence>
    fun observeMultiplePresence(userIds: List<String>): Flow<Map<String, UserPresence>>

    suspend fun setMyPresence(state: PresenceState): Boolean
    suspend fun setupOnDisconnect()
    suspend fun removePresence()
}
