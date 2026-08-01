package com.example.messenger.domain.usecase.message

import com.example.messenger.domain.model.Message
import com.example.messenger.domain.repository.IMessageRepository
import com.example.messenger.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val messageRepository: IMessageRepository
) {
    operator fun invoke(conversationId: String): Flow<Resource<List<Message>>> {
        if (conversationId.isBlank()) {
            return flow { emit(Resource.Error("Invalid conversation ID")) }
        }
        return messageRepository.getMessagesStream(conversationId)
            .map<List<Message>, Resource<List<Message>>> { Resource.Success(it) }
            .onStart { emit(Resource.Loading) }
            .catch { exception ->
                emit(Resource.Error(exception.message ?: "Failed to load messages"))
            }
    }
}
