package com.example.messenger.data.repository

import com.example.messenger.data.local.obx.ObxConversation
import com.example.messenger.data.local.obx.ObxConversation_
import com.example.messenger.data.local.obx.ObxMessage
import com.example.messenger.data.local.obx.ObxMessage_
import com.example.messenger.data.local.obx.ObxSyncQueueItem
import com.example.messenger.data.local.obx.asFlow
import com.example.messenger.data.local.obx.queryFlow
import com.example.messenger.data.local.obx.toDomain
import com.example.messenger.data.local.obx.toObx
import com.example.messenger.data.remote.firebase.FirestoreService
import com.example.messenger.domain.model.CallType
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.sync.SyncAction
import com.example.messenger.domain.repository.IMessageRepository
import io.objectbox.Box
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val messageBox: Box<ObxMessage>,
    private val conversationBox: Box<ObxConversation>,
    private val syncQueueBox: Box<ObxSyncQueueItem>,
    private val firestoreService: FirestoreService,
    private val authService: com.example.messenger.data.remote.auth.FirebaseAuthService,
) : IMessageRepository {

    override fun getMessagesStream(conversationId: String): Flow<List<Message>> {
        return queryFlow {
            messageBox.query(
                ObxMessage_.conversationId.equal(conversationId)
                    .and(ObxMessage_.deleted.equal(false)),
            )
                .order(ObxMessage_.timestamp)
                .build()
        }
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
    }

    private fun cutoffFor(conversationId: String): Long =
        conversationBox.query(ObxConversation_.uid.equal(conversationId))
            .build()
            .use { it.findFirst() }
            ?.clearedAtForMe ?: 0L

    override suspend fun sendMessage(message: Message): Result<Unit> {
        return try {
            
            messageBox.put(message.toObx())
            updateConversationPreview(message)

            val result = firestoreService.sendMessage(message.withStatus(MessageStatus.SENT))
            if (result.isFailure) {
                updateMessage(message.id) { it.status = MessageStatus.FAILED.name }
                enqueueRetry(message.id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            runCatching { updateMessage(message.id) { it.status = MessageStatus.FAILED.name } }
            runCatching { enqueueRetry(message.id) }
            Result.failure(e)
        }
    }

    override suspend fun persistIncomingMessage(message: Message) {
        val alreadyStored = messageBox.query(ObxMessage_.uid.equal(message.id)).build()
            .use { it.findFirst() } != null
        if (alreadyStored) return
        messageBox.put(message.toObx())
        updateConversationPreview(message)
    }

    private fun enqueueRetry(messageId: String, action: String = SyncAction.SEND) {
        syncQueueBox.put(
            ObxSyncQueueItem(
                entityType = com.example.messenger.domain.model.sync.SyncEntityType.MESSAGE,
                entityId = messageId,
                action = action,
                timestamp = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun deleteMessageForEveryone(message: Message): Result<Unit> {
        val currentUserId = authService.getCurrentUserId()
        if (currentUserId == null || message.senderId != currentUserId) {
            return Result.failure(SecurityException("Only own messages can be deleted for everyone"))
        }
        return try {
            updateMessage(message.id) { it.deleted = true }
            val result = firestoreService.deleteMessage(message)
            if (result.isFailure) {
                enqueueRetry(message.id, SyncAction.DELETE)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            runCatching { updateMessage(message.id) { it.deleted = true } }
            runCatching { enqueueRetry(message.id, SyncAction.DELETE) }
            Result.success(Unit)
        }
    }

    override suspend fun deleteMessageForMe(message: Message): Result<Unit> {
        return try {
            updateMessage(message.id) { it.deleted = true }
            val result = firestoreService.deleteMessageForMe(message)
            if (result.isFailure) {
                enqueueRetry(message.id, SyncAction.DELETE_FOR_ME)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            runCatching { updateMessage(message.id) { it.deleted = true } }
            runCatching { enqueueRetry(message.id, SyncAction.DELETE_FOR_ME) }
            Result.success(Unit)
        }
    }

    override suspend fun markMessageAsRead(message: Message): Result<Unit> {
        return try {
            val result = firestoreService.markMessageAsRead(message)
            if (result.isSuccess) {
                updateMessage(message.id) {
                    it.isRead = true
                    it.status = MessageStatus.READ.name
                }
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun observeRecentMessages(conversationId: String, limit: Long): Flow<Result<Unit>> = flow {
        try {
            firestoreService.getRecentMessagesStream(conversationId, limit, cutoffFor(conversationId)).collect { messages ->
                putPreservingReadState(conversationId, messages)
                emit(Result.success(Unit))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun loadOlderMessages(conversationId: String, limit: Long): Result<Int> {
        val oldestId = messageBox.query(ObxMessage_.conversationId.equal(conversationId))
            .order(ObxMessage_.timestamp)
            .build()
            .use { it.findFirst()?.uid }
            ?: return Result.success(0)
        return firestoreService.fetchOlderMessages(conversationId, oldestId, limit, cutoffFor(conversationId)).map { older ->
            putPreservingReadState(conversationId, older)
            older.size
        }
    }

    override suspend fun backfillRecentMessages(conversationId: String, limit: Long): Result<Int> {
        return firestoreService.fetchRecentMessagesOnce(conversationId, limit, cutoffFor(conversationId))
            .map { messages ->
                putPreservingReadState(conversationId, messages)
                messages.size
            }
    }

    private fun updateMessage(id: String, mutate: (ObxMessage) -> Unit) {
        val existing = messageBox.query(ObxMessage_.uid.equal(id)).build()
            .use { it.findFirst() } ?: return
        mutate(existing)
        messageBox.put(existing)
    }

    private fun putPreservingReadState(conversationId: String, messages: List<Message>) {
        if (messages.isEmpty()) return
        val localByUid = messageBox.query(ObxMessage_.conversationId.equal(conversationId))
            .build().use { it.find() }.associateBy { it.uid }
        val merged = messages.map { domain ->
            domain.toObx().also { incoming ->
                localByUid[incoming.uid]?.let { local ->
                    incoming.boxId = local.boxId
                    if (local.isRead) incoming.isRead = true
                    if (local.status == MessageStatus.READ.name) incoming.status = MessageStatus.READ.name
                    if (local.deleted) incoming.deleted = true
                }
            }
        }
        messageBox.put(merged)
    }

    private fun updateConversationPreview(message: Message) {
        val conv = conversationBox.query(ObxConversation_.uid.equal(message.conversationId)).build()
            .use { it.findFirst() } ?: return
        val label = previewLabel(message)
        conv.lastMessage = label
        conv.lastMessageSenderId = message.senderId
        conv.lastMessageTimestamp = message.timestamp
        conv.latestMessageText = label
        conv.latestMessageSenderId = message.senderId
        conv.latestMessageTimestamp = message.timestamp
        conversationBox.put(conv)
    }

    private fun previewLabel(message: Message): String = when (message) {
        is Message.Text  -> message.text
        is Message.Media -> message.caption.ifBlank {
            val count = message.items.size
            when {
                count == 1 && message.items.first().kind == MediaItem.VIDEO -> "Video"
                count == 1 -> "Photo"
                message.items.all { it.kind == MediaItem.IMAGE } -> "$count photos"
                message.items.all { it.kind == MediaItem.VIDEO } -> "$count videos"
                else -> "$count media"
            }
        }
        is Message.Call  -> when (message.callType) {
            CallType.MISSED    -> "Missed call"
            CallType.UNREACHED -> "Unreached call"
            CallType.ENDED     -> "Call"
        }
    }

    private fun Message.withStatus(newStatus: MessageStatus): Message = when (this) {
        is Message.Text  -> copy(status = newStatus)
        is Message.Media -> copy(status = newStatus)
        is Message.Call  -> copy(status = newStatus)
    }
}
