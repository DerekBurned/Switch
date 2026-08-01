package com.example.messenger.domain.usecase.presence

import com.example.messenger.domain.model.UserPresence
import com.example.messenger.domain.service.IPresenceService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveUserPresenceUseCase @Inject constructor(
    private val presenceService: IPresenceService
) {
    operator fun invoke(userId: String): Flow<UserPresence> {
        return presenceService.observePresence(userId)
    }

    fun observeMultiple(userIds: List<String>): Flow<Map<String, UserPresence>> {
        return presenceService.observeMultiplePresence(userIds)
    }
}
