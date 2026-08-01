package com.example.messenger.domain.usecase.typing

import com.example.messenger.domain.service.ITypingService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTypingUseCase @Inject constructor(
    private val typingService: ITypingService
) {
    operator fun invoke(conversationId: String): Flow<Set<String>> {
        return typingService.observeTyping(conversationId)
    }
}
