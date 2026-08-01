package com.example.messenger.data.remote.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeDbService @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val presenceRef = database.getReference("presence")
    private val typingRef = database.getReference("typing")
    private val connectedRef = database.getReference(".info/connected")

    fun observeConnectionState(): Flow<Boolean> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Boolean::class.java) ?: false)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        connectedRef.addValueEventListener(listener)
        awaitClose { connectedRef.removeEventListener(listener) }
    }

    suspend fun setPresence(userId: String, state: String, lastSeen: Long) {
        val data = mapOf(
            "state" to state,
            "lastSeen" to lastSeen,
            "lastChanged" to ServerValue.TIMESTAMP
        )
        presenceRef.child(userId).setValue(data).await()
    }

    fun setupOnDisconnect(userId: String) {
        val offlineData = mapOf(
            "state" to "offline",
            "lastSeen" to ServerValue.TIMESTAMP,
            "lastChanged" to ServerValue.TIMESTAMP
        )
        presenceRef.child(userId).onDisconnect().setValue(offlineData)
    }

    fun cancelOnDisconnect(userId: String) {
        presenceRef.child(userId).onDisconnect().cancel()
    }

    fun observePresence(userId: String): Flow<Map<String, Any?>> = callbackFlow {
        val ref = presenceRef.child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.value as? Map<String, Any?> ?: emptyMap()
                trySend(data)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun removePresence(userId: String) {
        presenceRef.child(userId).removeValue()
    }

    suspend fun setTyping(conversationId: String, userId: String) {
        val ref = typingRef.child(conversationId).child(userId)
        ref.onDisconnect().removeValue()
        ref.setValue(mapOf("timestamp" to ServerValue.TIMESTAMP)).await()
    }

    suspend fun clearTyping(conversationId: String, userId: String) {
        val ref = typingRef.child(conversationId).child(userId)
        ref.onDisconnect().cancel()
        ref.removeValue().await()
    }

    fun clearTypingNow(conversationId: String, userId: String) {
        val ref = typingRef.child(conversationId).child(userId)
        ref.onDisconnect().cancel()
        ref.removeValue()
    }

    fun observeTyping(conversationId: String): Flow<Map<String, Any?>> = callbackFlow {
        val ref = typingRef.child(conversationId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val typingUsers = mutableMapOf<String, Any?>()
                for (child in snapshot.children) {
                    child.key?.let { typingUsers[it] = child.value }
                }
                trySend(typingUsers)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
