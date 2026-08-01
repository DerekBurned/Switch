package com.example.messenger.domain.usecase.conversation

import com.example.messenger.domain.repository.IConversationRepository
import javax.inject.Inject

class MarkConversationAsReadUseCase @Inject constructor(
    private val conversationRepository: IConversationRepository
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> {
        if (conversationId.isBlank()) {
            return Result.failure(Exception("Invalid conversation ID"))
        }
        return conversationRepository.markConversationAsRead(conversationId)
    }
}
