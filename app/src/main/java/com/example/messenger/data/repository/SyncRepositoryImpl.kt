package com.example.messenger.data.repository

import android.util.Log
import com.example.messenger.data.local.obx.ObxMessage
import com.example.messenger.data.local.obx.ObxMessage_
import com.example.messenger.data.local.obx.ObxSyncQueueItem
import com.example.messenger.data.local.obx.ObxSyncQueueItem_
import com.example.messenger.data.local.obx.toDomain
import com.example.messenger.data.remote.firebase.FirestoreService
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.SyncStatus
import com.example.messenger.domain.model.sync.SyncAction
import com.example.messenger.domain.model.sync.SyncEntityType
import com.example.messenger.domain.repository.IConversationRepository
import com.example.messenger.domain.repository.ISyncRepository
import com.example.messenger.domain.repository.IUserRepository
import io.objectbox.Box
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class SyncRepositoryImpl @Inject constructor(
    private val syncQueueBox: Box<ObxSyncQueueItem>,
    private val messageBox: Box<ObxMessage>,
    private val firestoreService: FirestoreService,
    private val conversationRepository: IConversationRepository,
    private val userRepository: IUserRepository,
) : ISyncRepository {

    private companion object {
        const val TAG = "SyncRepository"
        const val MAX_RETRIES = 5
    }

    private val _syncStatus = MutableStateFlow(SyncStatus.SYNCED)

    override suspend fun syncAllData(): Result<Unit> {
        return try {
            _syncStatus.value = SyncStatus.SYNCING
            runCatching { conversationRepository.syncConversations() }
                .onFailure { Log.w(TAG, "Conversation backfill failed", it) }
            runCatching { userRepository.refreshContactAliases() }
                .onFailure { Log.w(TAG, "Contact alias backfill failed", it) }
            processSyncQueue()
            _syncStatus.value = SyncStatus.SYNCED
            Result.success(Unit)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.FAILED
            Result.failure(e)
        }
    }

    override suspend fun processSyncQueue(): Result<Unit> {
        return try {
            val pending = syncQueueBox.query().order(ObxSyncQueueItem_.timestamp).build()
                .use { it.find() }
            for (item in pending) {
                when (item.entityType) {
                    SyncEntityType.MESSAGE -> handleMessageItem(item)
                    else -> {
                        Log.w(TAG, "Dropping unknown sync entity type: ${item.entityType}")
                        syncQueueBox.remove(item)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun handleMessageItem(item: ObxSyncQueueItem) {
        when (item.action) {
            SyncAction.SEND -> retrySend(item)
            SyncAction.DELETE -> retryDelete(item)
            SyncAction.DELETE_FOR_ME -> retryDelete(item, forMe = true)
            else -> {
                Log.w(TAG, "Dropping message item with unknown action: ${item.action}")
                syncQueueBox.remove(item)
            }
        }
    }

    private suspend fun retryDelete(item: ObxSyncQueueItem, forMe: Boolean = false) {
        val entity = messageBox.query(ObxMessage_.uid.equal(item.entityId)).build()
            .use { it.findFirst() }
        if (entity == null) {
            syncQueueBox.remove(item)
            return
        }
        val result = if (forMe) {
            firestoreService.deleteMessageForMe(entity.toDomain())
        } else {
            firestoreService.deleteMessage(entity.toDomain())
        }
        if (result.isSuccess) {
            syncQueueBox.remove(item)
        } else {
            if (item.retryCount + 1 >= MAX_RETRIES) {
                Log.w(TAG, "Dropping delete ${item.entityId} after $MAX_RETRIES retries")
                syncQueueBox.remove(item)
            } else {
                item.retryCount += 1
                syncQueueBox.put(item)
            }
        }
    }

    private suspend fun retrySend(item: ObxSyncQueueItem) {
        val entity = messageBox.query(ObxMessage_.uid.equal(item.entityId)).build()
            .use { it.findFirst() }
        if (entity == null) {
            syncQueueBox.remove(item)
            return
        }
        val msg = entity.toDomain()
        val message = when (msg) {
            is com.example.messenger.domain.model.Message.Text  -> msg.copy(status = MessageStatus.SENT)
            is com.example.messenger.domain.model.Message.Media -> msg.copy(status = MessageStatus.SENT)
            is com.example.messenger.domain.model.Message.Call  -> msg.copy(status = MessageStatus.SENT)
        }
        val result = firestoreService.sendMessage(message)
        if (result.isSuccess) {
            entity.status = MessageStatus.SENT.name
            messageBox.put(entity)
            syncQueueBox.remove(item)
        } else {
            if (item.retryCount + 1 >= MAX_RETRIES) {
                Log.w(TAG, "Dropping message ${item.entityId} after $MAX_RETRIES retries")
                syncQueueBox.remove(item)
            } else {
                item.retryCount += 1
                syncQueueBox.put(item)
            }
        }
    }

    override fun observeSyncStatus(): Flow<SyncStatus> = _syncStatus

    override suspend fun getSyncQueueCount(): Int = syncQueueBox.count().toInt()
}
