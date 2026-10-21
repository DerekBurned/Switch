package com.example.messenger.domain.usecase.sync

import com.example.messenger.domain.repository.IConversationRepository
import com.example.messenger.domain.repository.IMessageRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class BackfillRecentHistoryUseCase @Inject constructor(
    private val conversationRepository: IConversationRepository,
    private val messageRepository: IMessageRepository,
) {

    suspend operator fun invoke(
        conversationLimit: Int = CONVERSATION_LIMIT,
        messageLimit: Long = MESSAGE_LIMIT,
    ): Result<Int> = runCatching {
        conversationRepository.syncConversations()
        conversationRepository.getAllConversations().first()
            .sortedByDescending { it.lastMessageTimestamp }
            .take(conversationLimit)
            .sumOf { conversation ->
                messageRepository.backfillRecentMessages(conversation.id, messageLimit)
                    .getOrDefault(0)
            }
    }

    companion object {
        const val CONVERSATION_LIMIT = 30
        const val MESSAGE_LIMIT = 50L
    }
}
