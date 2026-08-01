package com.example.messenger.data.repository

import android.util.Log
import com.example.messenger.data.local.obx.ObxConversation
import com.example.messenger.data.local.obx.ObxConversation_
import com.example.messenger.data.local.obx.ObxMessage
import com.example.messenger.data.local.obx.ObxMessage_
import com.example.messenger.data.local.obx.asFlow
import com.example.messenger.data.local.obx.toDomain
import com.example.messenger.data.local.obx.toObx
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.data.remote.firebase.FirestoreService
import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.model.Profile
import com.example.messenger.domain.repository.IConversationRepository
import io.objectbox.Box
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class ConversationRepositoryImpl @Inject constructor(
    private val firestoreService: FirestoreService,
    private val conversationBox: Box<ObxConversation>,
    private val messageBox: Box<ObxMessage>,
    private val authService: FirebaseAuthService,
) : IConversationRepository {

    override fun getAllConversations(): Flow<List<Conversation>> = channelFlow {
        val remoteJob = launch {
            val userId = authService.getCurrentUserId()
            if (userId.isNullOrBlank()) {
                Log.w(TAG, "getAllConversations: no signed-in user; skipping remote listener")
                return@launch
            }
            try {
                firestoreService.getAllConversations(userId).collect { sync ->
                    sync.conversations.forEach { upsert(it) }
                    if (!sync.fromCache && sync.removedIds.isNotEmpty()) {
                        prune(sync.removedIds)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Remote conversation listener errored", e)
            }
        }
        val localJob = launch {
            conversationBox.query()
                .orderDesc(ObxConversation_.lastMessageTimestamp)
                .build()
                .asFlow()
                .map { rows -> rows.map { it.toDomain() } }
                .collect { send(it) }
        }
        awaitClose {
            remoteJob.cancel()
            localJob.cancel()
        }
    }

    override suspend fun getConversationById(conversationId: String): Result<Conversation?> {
        return try {
            Result.success(findByUid(conversationId)?.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getConversationsForProfile(profile: Profile): Result<List<Conversation>> {
        return try {
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createConversation(participantIds: List<String>): Result<Conversation> {
        return try {
            val currentUserId = authService.getCurrentUserId()
            if (currentUserId.isNullOrBlank()) {
                return Result.failure(
                    IllegalStateException("Cannot create conversation: no signed-in user"),
                )
            }

            val existing = firestoreService
                .findExistingConversation(participantIds, currentUserId)
                .getOrElse { return Result.failure(it) }
            if (existing != null) {
                upsert(existing)
                return Result.success(existing)
            }

            val names = mutableListOf<String>()
            val avatars = mutableListOf<String?>()
            for (id in participantIds) {
                val user = firestoreService.getUserProfile(id).getOrNull()
                names.add(user?.username ?: "Unknown")
                avatars.add(user?.avatarUrl)
            }
            val conversation = Conversation(
                id = "",
                participantIds = participantIds,
                participantNames = names,
                participantAvatars = avatars,
                lastMessage = null,
                lastMessageTimestamp = System.currentTimeMillis(),
            )

            val firestoreId = firestoreService.createConversation(conversation).getOrThrow()
            val savedConversation = conversation.copy(id = firestoreId)
            upsert(savedConversation)
            Result.success(savedConversation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> {
        return try {
            conversationBox.query(ObxConversation_.uid.equal(conversationId)).build()
                .use { it.remove() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteConversationForEveryone(conversationId: String): Result<Unit> {
        return try {
            firestoreService.deleteConversation(conversationId).getOrThrow()
            prune(listOf(conversationId))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "deleteConversationForEveryone failed", e)
            Result.failure(e)
        }
    }

    override suspend fun clearConversationForMe(conversationId: String): Result<Unit> {
        return try {
            val userId = authService.getCurrentUserId()
            if (userId.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Cannot clear conversation: no signed-in user"))
            }
            val local = findByUid(conversationId)
                ?: return Result.failure(IllegalStateException("Conversation not found: $conversationId"))
            val cutoff = maxOf(local.lastMessageTimestamp, local.latestMessageTimestamp)
            firestoreService.clearConversationForMe(conversationId, userId, cutoff).getOrThrow()
            prune(listOf(conversationId))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "clearConversationForMe failed", e)
            Result.failure(e)
        }
    }

    override suspend fun markConversationAsRead(conversationId: String): Result<Unit> {
        return try {
            val userId = authService.getCurrentUserId()
            findByUid(conversationId)?.let { conv ->
                conv.unreadCount = 0
                conversationBox.put(conv)
            }
            if (!userId.isNullOrBlank()) {
                messageBox.query(
                    ObxMessage_.conversationId.equal(conversationId)
                        .and(ObxMessage_.senderId.notEqual(userId))
                        .and(ObxMessage_.isRead.equal(false)),
                ).build().use { query ->
                    val unread = query.find()
                    if (unread.isNotEmpty()) {
                        unread.forEach { it.isRead = true }
                        messageBox.put(unread)
                    }
                }
                firestoreService.resetUnreadCount(conversationId, userId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markConversationAsUnread(conversationId: String): Result<Unit> {
        return try {
            val userId = authService.getCurrentUserId()
            if (!userId.isNullOrBlank()) {
                messageBox.query(
                    ObxMessage_.conversationId.equal(conversationId)
                        .and(ObxMessage_.senderId.notEqual(userId))
                        .and(ObxMessage_.deleted.equal(false)),
                ).build().use { query ->
                    val latest = query.find().maxByOrNull { it.timestamp }
                    if (latest != null && latest.isRead) {
                        latest.isRead = false
                        messageBox.put(latest)
                    }
                }
            }
            findByUid(conversationId)?.let { conv ->
                conv.unreadCount = maxOf(conv.unreadCount, 1)
                conversationBox.put(conv)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun observeRemoteConversations(): Flow<List<Conversation>> {
        val userId = authService.getCurrentUserId() ?: ""
        return firestoreService.getAllConversations(userId)
            .onEach { sync ->
                sync.conversations.forEach { upsert(it) }
                if (!sync.fromCache && sync.removedIds.isNotEmpty()) prune(sync.removedIds)
            }
            .map { it.conversations }
    }

    override suspend fun syncConversations() {
        try {
            val userId = authService.getCurrentUserId()
            if (userId.isNullOrBlank()) {
                Log.w(TAG, "syncConversations: no signed-in user")
                return
            }
            val result = firestoreService.fetchAllConversationsOnce(userId)
            val remote = result.getOrNull()
            if (remote == null) {
                Log.w(TAG, "syncConversations: server fetch failed; skipping prune", result.exceptionOrNull())
                return
            }
            remote.forEach { upsert(it) }
            val staleIds = ConversationReconciler.staleIds(
                local = conversationBox.all.map { it.uid }.toSet(),
                remote = RemoteConversations.Server(remote.map { it.id }.toSet()),
            )
            prune(staleIds)
        } catch (e: Exception) {
            Log.w(TAG, "Conversation sync failed (SyncWorker will retry)", e)
        }
    }

    private fun findByUid(uid: String): ObxConversation? =
        conversationBox.query(ObxConversation_.uid.equal(uid)).build().use { it.findFirst() }

    private fun localUnread(conversationId: String, me: String): Int {
        if (me.isBlank()) return 0
        return messageBox.query(
            ObxMessage_.conversationId.equal(conversationId)
                .and(ObxMessage_.senderId.notEqual(me))
                .and(ObxMessage_.isRead.equal(false))
                .and(ObxMessage_.deleted.equal(false)),
        ).build().use { it.count().toInt() }
    }

    private fun prune(conversationIds: Collection<String>) {
        if (conversationIds.isEmpty()) return
        for (id in conversationIds) {
            conversationBox.query(ObxConversation_.uid.equal(id)).build().use { it.remove() }
            messageBox.query(ObxMessage_.conversationId.equal(id)).build().use { it.remove() }
        }
        Log.d(TAG, "Pruned ${conversationIds.size} remote-deleted conversation(s)")
    }

    private fun upsert(conversation: Conversation) {
        val incoming = conversation.toObx()
        val currentUserId = authService.getCurrentUserId()
        val serverClearedAt = currentUserId?.let { conversation.clearedAt[it] } ?: 0L
        val me = currentUserId ?: ""
        val serverUnread = conversation.unreadCounts[me] ?: 0
        incoming.unreadCount = maxOf(serverUnread, localUnread(conversation.id, me))
        findByUid(conversation.id)?.let { existing ->
            incoming.boxId = existing.boxId
            incoming.latestMessageText = existing.latestMessageText
            incoming.latestMessageSenderId = existing.latestMessageSenderId
            incoming.latestMessageTimestamp = existing.latestMessageTimestamp
        }
        incoming.clearedAtForMe = serverClearedAt
        conversationBox.put(incoming)
    }

    private companion object {
        const val TAG = "ConversationRepo"
    }
}
