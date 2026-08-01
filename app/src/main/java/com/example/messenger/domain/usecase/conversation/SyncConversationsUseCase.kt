package com.example.messenger.domain.usecase.conversation

import com.example.messenger.domain.repository.IConversationRepository
import javax.inject.Inject

class SyncConversationsUseCase @Inject constructor(
    private val conversationRepository: IConversationRepository
) {
    suspend operator fun invoke() {
        conversationRepository.syncConversations()
    }
}