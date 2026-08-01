package com.example.messenger.domain.usecase.conversation

import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.repository.IConversationRepository
import com.example.messenger.domain.usecase.auth.GetCurrentUserUseCase
import javax.inject.Inject

class CreateConversationUseCase @Inject constructor(
    private val conversationRepository: IConversationRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) {
    suspend operator fun invoke(participantId: String): Result<Conversation> {
        
        val currentUser = getCurrentUserUseCase()
            ?: return Result.failure(Exception("User not logged in"))

        if (participantId.isBlank()) {
            return Result.failure(Exception("Invalid participant ID"))
        }

        if (participantId == currentUser.id) {
            return Result.failure(Exception("Cannot create conversation with yourself"))
        }

        val participantIds = listOf(currentUser.id, participantId)
        return conversationRepository.createConversation(participantIds)
    }
}