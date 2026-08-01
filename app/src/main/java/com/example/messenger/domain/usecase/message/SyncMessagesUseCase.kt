package com.example.messenger.domain.usecase.message

import com.example.messenger.domain.repository.IMessageRepository
import com.example.messenger.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SyncMessagesUseCase @Inject constructor(
    private val messageRepository: IMessageRepository
) {
    suspend operator fun invoke(conversationId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading)
            messageRepository.observeRecentMessages(conversationId).collect { result ->
                result.fold(
                    onSuccess = { emit(Resource.Success(Unit)) },
                    onFailure = { e -> emit(Resource.Error(e.message ?: "Sync failed")) }
                )
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Sync error"))
        }
    }
}
