package com.example.messenger.domain.usecase.message

import com.example.messenger.domain.repository.IMediaRepository
import javax.inject.Inject

class SendMediaMessageUseCase @Inject constructor(
    private val mediaRepository: IMediaRepository,
) {
    operator fun invoke(conversationId: String, uris: List<String>, caption: String = "") {
        if (conversationId.isBlank() || uris.isEmpty()) return
        mediaRepository.sendMediaMessage(conversationId, uris, caption)
    }
}
