package com.example.messenger.data.remote.call

import com.example.messenger.data.remote.dto.CallSignalDto
import com.example.messenger.data.remote.dto.toCallSignal
import com.example.messenger.data.remote.dto.toDto
import com.example.messenger.domain.model.CallSignal
import com.example.messenger.domain.model.CallStatus
import com.example.messenger.domain.service.ICallSignalingService
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseCallSignalingService @Inject constructor(
    private val db: FirebaseDatabase,
) : ICallSignalingService {

    private val callsRef get() = db.reference.child("calls")
    private val callAcksRef get() = db.reference.child("callAcks")
    private val callMembersRef get() = db.reference.child("callMembers")

    override suspend fun sendCallSignal(signal: CallSignal) {
        callsRef
            .child(signal.calleeId)
            .child(signal.callId)
            .setValue(signal.toDto())
            .await()
    }

    override fun observeIncomingCall(myUserId: String): Flow<CallSignal?> = callbackFlow {
        val inbox = callsRef.child(myUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val now = System.currentTimeMillis()
                val signal = snapshot.children
                    .mapNotNull { it.getValue(CallSignalDto::class.java)?.toCallSignal() }

                    .filter { it.status == CallStatus.RINGING && now - it.timestamp < RINGING_TTL_MS }
                    .maxByOrNull { it.timestamp }
                trySend(signal)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }
        inbox.addValueEventListener(listener)
        awaitClose { inbox.removeEventListener(listener) }
    }

    override suspend fun updateCallStatus(
        calleeId: String,
        callId: String,
        status: CallStatus,
    ) {
        callsRef
            .child(calleeId)
            .child(callId)
            .child("status")
            .setValue(status.name)
            .await()
    }

    override fun observeCallStatus(calleeId: String, callId: String): Flow<CallStatus?> =
        callbackFlow {
            val ref = callsRef.child(calleeId).child(callId).child("status")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val raw = snapshot.getValue(String::class.java)
                    val status = raw?.let { name ->
                        runCatching { CallStatus.valueOf(name) }.getOrNull()
                    }
                    trySend(status)
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(null)
                }
            }
            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }

    override suspend fun clearCall(calleeId: String, callId: String) {
        callsRef
            .child(calleeId)
            .child(callId)
            .removeValue()
            .await()
    }

    override suspend fun ackRinging(callerId: String, callId: String, calleeId: String) {
        callAcksRef
            .child(callerId)
            .child(callId)
            .setValue(mapOf("calleeId" to calleeId, "ringingAt" to System.currentTimeMillis()))
            .await()
    }

    override fun observeRingingAck(callerId: String, callId: String): Flow<Boolean> =
        callbackFlow {
            val ref = callAcksRef.child(callerId).child(callId)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    trySend(snapshot.exists())
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(false)
                }
            }
            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }

    override suspend fun clearRingingAck(callerId: String, callId: String) {
        callAcksRef
            .child(callerId)
            .child(callId)
            .removeValue()
            .await()
    }

    override suspend fun joinCallPresence(callId: String, uid: String) {
        val ref = callMembersRef.child(callId).child(uid)
        ref.onDisconnect().removeValue()
        ref.setValue(true).await()
    }

    override fun leaveCallPresence(callId: String, uid: String) {
        val ref = callMembersRef.child(callId).child(uid)
        ref.onDisconnect().cancel()
        ref.removeValue()
    }

    override fun observeCallMembers(callId: String): Flow<Set<String>> = callbackFlow {
        val ref = callMembersRef.child(callId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.children.mapNotNull { it.key }.toSet())
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptySet())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private companion object {
        
        const val RINGING_TTL_MS = 60_000L
    }
}
