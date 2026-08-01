package com.example.messenger.domain.usecase.message

import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.repository.IMessageRepository
import com.example.messenger.domain.usecase.auth.GetCurrentUserUseCase
import com.example.messenger.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val messageRepository: IMessageRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) {
    suspend operator fun invoke(
        conversationId: String,
        text: String,
        replyTo: Message? = null,
    ): Flow<Resource<Unit>> = flow {
        
       try {
            if (text.isBlank()) {
                emit(Resource.Error("Message is empty"))
            }

            if (text.length > 5000) {
                emit(Resource.Error("Message is too long"))
            }

            val currentUser = getCurrentUserUseCase()

           if(currentUser == null) {
               emit(Resource.Error("User not logged in"))
               return@flow
           }

            val message = Message.Text(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = currentUser.id,
                text = text.trim(),
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENDING,
                replyToMessageId = replyTo?.id,
                replyToText = when (replyTo) {
                    is Message.Text  -> replyTo.text.take(REPLY_SNIPPET_MAX)
                    is Message.Media -> replyTo.caption.take(REPLY_SNIPPET_MAX).ifBlank { "Media" }
                    is Message.Call, null -> null
                },
                replyToSenderId = replyTo?.senderId,
            )
           val result = messageRepository.sendMessage(message)
           result.fold(
               onSuccess = {
                   emit(Resource.Success(Unit))
               },
               onFailure = { e ->
                   emit(Resource.Error(e.message ?: "Failed to send message"))
               }
           )
        }catch (e : Exception){
            emit(Resource.Error(e.message?: "Internal error occurred"))
        }
    }

    private companion object {
        
        const val REPLY_SNIPPET_MAX = 200
    }
}