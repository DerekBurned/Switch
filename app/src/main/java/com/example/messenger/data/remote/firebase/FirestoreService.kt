package com.example.messenger.data.remote.firebase

import android.util.Log
import com.example.messenger.data.crypto.E2eeMessageCodec
import com.example.messenger.data.remote.dto.RemoteMessageDto
import com.example.messenger.domain.model.CallType
import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.User
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val codec: E2eeMessageCodec,
    private val authService: com.example.messenger.data.remote.auth.FirebaseAuthService,
) {
    private val usersCollection = firestore.collection("users")
    private val conversationsCollection = firestore.collection("conversations")

    suspend fun createUserProfile(user: User): Result<Unit> {
        Log.d("AUTHFLOW_FS", "createUserProfile: id=${user.id} username=${user.username}")
        return try {
            
            usersCollection.document(user.id).set(user).await()
            Log.d("AUTHFLOW_FS", "createUserProfile: OK")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AUTHFLOW_FS", "createUserProfile: FAILED", e)
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): Result<User> {
        Log.d("AUTHFLOW_FS", "getUserProfile: uid=$uid")
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            val user = snapshot.toObject(User::class.java)
            Log.d("AUTHFLOW_FS", "getUserProfile: exists=${snapshot.exists()} parsed=${user != null}")
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User profile not found"))
            }
        } catch (e: Exception) {
            Log.e("AUTHFLOW_FS", "getUserProfile: FAILED", e)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            usersCollection.document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(uid: String, token: String): Result<Unit> {
        return try {
            
            val updates = mapOf("fcmToken" to token)
            usersCollection.document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFcmToken(uid: String): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .update("fcmToken", com.google.firebase.firestore.FieldValue.delete())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("AUTHFLOW_FS", "deleteFcmToken failed", e)
            Result.failure(e)
        }
    }
    suspend fun sendMessage(message: Message): Result<Unit> {
        return try {
            val dto = codec.encode(message).let {
                if (message.status == MessageStatus.SENDING) it.copy(status = MessageStatus.SENT.name) else it
            }
            val conversationRef = conversationsCollection.document(message.conversationId)
            conversationRef
                .collection("messages")
                .document(dto.id)
                .set(dto)
            conversationRef
                .update(
                    mapOf(
                        "lastMessage" to if (dto.enc == 1) encryptedPreview(message) else previewLabel(message),
                        "lastMessageSenderId" to message.senderId,
                        "lastMessageTimestamp" to message.timestamp,
                    ),
                )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error sending message", e)
            Result.failure(e)
        }
    }

    private fun encryptedPreview(message: Message): String = when (message) {
        is Message.Text -> "Message"
        is Message.Media -> previewLabel(message.copy(caption = ""))
        is Message.Call -> previewLabel(message)
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

    suspend fun deleteMessageForMe(message: Message): Result<Unit> {
        return try {
            val uid = authService.getCurrentUserId()
                ?: return Result.failure(Exception("Not signed in"))
            conversationsCollection
                .document(message.conversationId)
                .collection("messages")
                .document(message.id)
                .update("deletedFor", com.google.firebase.firestore.FieldValue.arrayUnion(uid))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error deleting message for me", e)
            Result.failure(e)
        }
    }

    private fun Message.markDeleted(): Message = when (this) {
        is Message.Text  -> copy(deleted = true)
        is Message.Media -> copy(deleted = true)
        is Message.Call  -> copy(deleted = true)
    }

    private fun applyDeletedFor(dto: RemoteMessageDto, message: Message): Message {
        val uid = authService.getCurrentUserId()
        return if (uid != null && dto.deletedFor.contains(uid)) message.markDeleted() else message
    }

    suspend fun deleteMessage(message: Message): Result<Unit>{
        return try {
            conversationsCollection
                .document(message.conversationId)
                .collection("messages")
                .document(message.id)
                .update("deleted", true)
                .await()
            Result.success(Unit)
        }catch (e: Exception)
        {
            Log.e("FirestoreService", "Error deleting message", e)
            Result.failure(e)
        }
    }
    suspend fun markMessageAsRead(message: Message): Result<Unit>{
        return try{
            conversationsCollection
                .document(message.conversationId)
                .collection("messages")
                .document(message.id)
                .update("status", MessageStatus.READ.name)
                .await()
            Log.d("FirestoreService", "Message marked as read")
            Result.success(Unit)
        }catch (e: Exception){
            Log.e("FirestoreService", "Error marking message as read", e)
            Result.failure(e)
        }
    }

    fun getRecentMessagesStream(
        conversationId: String,
        limit: Long = 100,
        cutoff: Long = 0,
    ): Flow<List<Message>> {
        val raw: Flow<List<Pair<RemoteMessageDto, Boolean>>> = callbackFlow {
            val messagesRef = conversationsCollection
                .document(conversationId)
                .collection("messages")
                .whereGreaterThan("timestamp", cutoff)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)

            val listener = messagesRef.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    Log.w("FirestoreService", "Listen failed.", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val rows = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(RemoteMessageDto::class.java)
                            ?.copy(id = doc.id)
                            ?.let { it to doc.metadata.hasPendingWrites() }
                    }.reversed()
                    trySend(rows)
                }
            }
            awaitClose { listener.remove() }
        }
        return raw.map { rows -> rows.map { (dto, pending) -> applyDeletedFor(dto, codec.decode(dto, pending)) } }
    }

    suspend fun fetchRecentMessagesOnce(
        conversationId: String,
        limit: Long = 30,
        cutoff: Long = 0,
    ): Result<List<Message>> {
        return try {
            val snapshot = conversationsCollection
                .document(conversationId)
                .collection("messages")
                .whereGreaterThan("timestamp", cutoff)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            val messages = snapshot.documents.mapNotNull { doc ->
                doc.toObject(RemoteMessageDto::class.java)?.copy(id = doc.id)
            }.map { dto -> applyDeletedFor(dto, codec.decode(dto)) }.reversed()
            Result.success(messages)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error fetching recent messages", e)
            Result.failure(e)
        }
    }

    suspend fun fetchOlderMessages(
        conversationId: String,
        oldestLoadedMessageId: String,
        limit: Long = 100,
        cutoff: Long = 0,
    ): Result<List<Message>> {
        return try {
            val messagesCollection = conversationsCollection
                .document(conversationId)
                .collection("messages")
            val anchor = messagesCollection.document(oldestLoadedMessageId).get().await()
            if (!anchor.exists()) {
                return Result.success(emptyList())
            }
            val snapshot = messagesCollection
                .whereGreaterThan("timestamp", cutoff)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(anchor)
                .limit(limit)
                .get()
                .await()
            val messages = snapshot.documents.mapNotNull { doc ->
                doc.toObject(RemoteMessageDto::class.java)?.copy(id = doc.id)
            }.map { dto -> applyDeletedFor(dto, codec.decode(dto)) }.reversed()
            Result.success(messages)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error fetching older messages", e)
            Result.failure(e)
        }
    }
    suspend fun getMessageOnce(conversationId: String, messageId: String): Result<RemoteMessageDto> {
        return try {
            val doc = conversationsCollection.document(conversationId)
                .collection("messages")
                .document(messageId)
                .get()
                .await()
            val dto = doc.toObject(RemoteMessageDto::class.java)?.copy(id = doc.id)
            if (dto != null) Result.success(dto) else Result.failure(Exception("Message not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val prefix = query.trim().lowercase()
            val snapshot = usersCollection
                .whereGreaterThanOrEqualTo("usernameLower", prefix)
                .whereLessThanOrEqualTo("usernameLower", prefix + "")
                .limit(20)
                .get()
                .await()
            val users = snapshot.toObjects(User::class.java)
            Result.success(users)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error searching users", e)
            Result.failure(e)
        }
    }

    suspend fun setContactAlias(myUid: String, contactId: String, name: String): Result<Unit> {
        return try {
            usersCollection.document(myUid)
                .collection("contactAliases")
                .document(contactId)
                .set(mapOf("name" to name, "updatedAt" to System.currentTimeMillis()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error setting contact alias", e)
            Result.failure(e)
        }
    }

    suspend fun getContactAliases(myUid: String): Result<Map<String, String>> {
        return try {
            val snapshot = usersCollection.document(myUid)
                .collection("contactAliases")
                .get()
                .await()
            val aliases = snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                doc.id to name
            }.toMap()
            Result.success(aliases)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error fetching contact aliases", e)
            Result.failure(e)
        }
    }

    suspend fun findExistingConversation(
        participantIds: List<String>,
        currentUserId: String,
    ): Result<Conversation?> {
        return try {

            val snapshot = conversationsCollection
                .whereArrayContains("participantIds", currentUserId)
                .get()
                .await()
            val conversations = snapshot.toObjects(Conversation::class.java).mapIndexed { index, conversation ->
                conversation.copy(id = snapshot.documents[index].id)
            }
            val existing = conversations.find { conv ->
                conv.participantIds.size == 2 && conv.participantIds.containsAll(participantIds)
            }
            Result.success(existing)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error finding existing conversation", e)
            Result.failure(e)
        }
    }

    suspend fun createConversation(conversation: Conversation): Result<String> {
        return try {
            val seeded = conversation.copy(visibleTo = conversation.participantIds)
            val docRef = conversationsCollection.add(seeded).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error creating conversation", e)
            Result.failure(e)
        }
    }

    suspend fun deleteConversation(conversationId: String): Result<Unit> {
        return try {
            conversationsCollection.document(conversationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error deleting conversation", e)
            Result.failure(e)
        }
    }

    suspend fun clearConversationForMe(
        conversationId: String,
        userId: String,
        clearedAt: Long,
    ): Result<Unit> {
        return try {
            conversationsCollection.document(conversationId)
                .update(
                    "clearedAt.$userId", clearedAt,
                    "visibleTo", com.google.firebase.firestore.FieldValue.arrayRemove(userId),
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error clearing conversation for user", e)
            Result.failure(e)
        }
    }

    suspend fun resetUnreadCount(conversationId: String, userId: String): Result<Unit> {
        return try {
            conversationsCollection.document(conversationId)
                .update("unreadCounts.$userId", 0)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error resetting unread count", e)
            Result.failure(e)
        }
    }

    suspend fun fetchAllConversationsOnce(userId: String): Result<List<Conversation>> {
        return try {
            val snapshot = conversationsCollection
                .whereArrayContains("visibleTo", userId)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                .get(Source.SERVER)
                .await()
            val conversations = snapshot.toObjects(Conversation::class.java)
                .mapIndexed { index, conversation ->
                    conversation.copy(id = snapshot.documents[index].id)
                }
            Result.success(conversations)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error fetching conversations once", e)
            Result.failure(e)
        }
    }

    fun getAllConversations(userId: String): Flow<ConversationSync> = callbackFlow {
        val conversationsRef = conversationsCollection
            .whereArrayContains("visibleTo", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)

        val listener = conversationsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("FirestoreService", "Listen failed.", error)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener

            val conversations = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Conversation::class.java)?.copy(id = doc.id)
            }
            val removedIds = snapshot.documentChanges
                .filter { it.type == DocumentChange.Type.REMOVED }
                .map { it.document.id }
            trySend(ConversationSync(conversations, removedIds, snapshot.metadata.isFromCache))
        }
        awaitClose { listener.remove() }
    }

}