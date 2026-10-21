package com.example.messenger.testutil.fakes

import com.example.messenger.domain.model.presence.PresenceState
import com.example.messenger.domain.model.presence.UserPresence
import com.example.messenger.domain.service.IPresenceService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePresenceService : IPresenceService {

    val presence = MutableStateFlow(UserPresence())
    val multiple = MutableStateFlow<Map<String, UserPresence>>(emptyMap())

    override fun observePresence(userId: String): Flow<UserPresence> = presence

    override fun observeMultiplePresence(userIds: List<String>): Flow<Map<String, UserPresence>> = multiple

    override suspend fun setMyPresence(state: PresenceState): Boolean = true

    override suspend fun setupOnDisconnect() = Unit

    override suspend fun removePresence() = Unit
}
