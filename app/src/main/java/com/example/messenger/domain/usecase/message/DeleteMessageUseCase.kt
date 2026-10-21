package com.example.messenger.domain.usecase.message

import com.example.messenger.domain.model.Message
import com.example.messenger.domain.repository.IClipRepository
import com.example.messenger.domain.repository.IMessageRepository
import com.example.messenger.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class DeleteMessageUseCase @Inject constructor(
    private val messageRepository: IMessageRepository,
    private val clipRepository: IClipRepository,
) {
    suspend operator fun invoke(message: Message, forEveryone: Boolean): Flow<Resource<Unit>> = flow  {
       try {
           emit(Resource.Loading)
            val result = if (forEveryone) {
                messageRepository.deleteMessageForEveryone(message)
            } else {
                messageRepository.deleteMessageForMe(message)
            }
           result.fold(
                onSuccess = {
                    if (forEveryone && message is Message.Clip) clipRepository.deleteRemoteFile(message)
                    emit(Resource.Success(Unit))
                },
                onFailure = { e ->
                    emit(Resource.Error(e.message ?: "Failed to delete the message"))
                }
            )
        }catch (
            e: Exception
        ){
            emit(Resource.Error("Internal error occurred"))
        }
    }
}