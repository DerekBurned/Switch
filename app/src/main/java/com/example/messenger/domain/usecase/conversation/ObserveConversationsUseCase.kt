package com.example.messenger.domain.usecase.conversation

import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.repository.IConversationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConversationsUseCase  @Inject constructor(
    private val conversationRepository: IConversationRepository
) {
    suspend operator fun invoke(): Flow<List<Conversation>> {
        return conversationRepository.observeRemoteConversations()
    }
}