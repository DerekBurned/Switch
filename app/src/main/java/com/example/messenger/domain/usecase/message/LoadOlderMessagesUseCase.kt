package com.example.messenger.domain.usecase.message

import com.example.messenger.domain.repository.IMessageRepository
import javax.inject.Inject

class LoadOlderMessagesUseCase @Inject constructor(
    private val messageRepository: IMessageRepository
) {
    
    suspend operator fun invoke(conversationId: String, limit: Long = 100): Result<Int> {
        if (conversationId.isBlank()) return Result.success(0)
        return messageRepository.loadOlderMessages(conversationId, limit)
    }
}
