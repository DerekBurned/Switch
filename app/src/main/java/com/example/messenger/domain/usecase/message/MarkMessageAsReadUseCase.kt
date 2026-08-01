package com.example.messenger.domain.usecase.message

import android.util.Log
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.repository.IMessageRepository
import com.example.messenger.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MarkMessageAsReadUseCase @Inject constructor(
    private val messageRepository: IMessageRepository
) {
    suspend operator fun invoke(message: Message): Resource<Unit> {
        return try {
            messageRepository.markMessageAsRead(message)
                .fold(
                    onSuccess = {
                        Log.d("MarkMessageAsReadUseCase", "Success marking message as read id: ${message.id}")
                        Resource.Success(Unit)
                    },
                    onFailure = { e ->
                        Log.d("MarkMessageAsReadUseCase", "Error marking message as read: $e")
                        Resource.Error(e.toString())
                    }
                )
        } catch (e: Exception) {
            Log.e("MarkMessageAsReadUseCase", "Exception in use case: $e")
            Resource.Error(e.toString())
        }
    }
}