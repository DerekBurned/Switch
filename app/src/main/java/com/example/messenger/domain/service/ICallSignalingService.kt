package com.example.messenger.domain.service

import com.example.messenger.domain.model.CallSignal
import com.example.messenger.domain.model.CallStatus
import kotlinx.coroutines.flow.Flow

interface ICallSignalingService {
    
    suspend fun sendCallSignal(signal: CallSignal)

    fun observeIncomingCall(myUserId: String): Flow<CallSignal?>

    suspend fun updateCallStatus(calleeId: String, callId: String, status: CallStatus)

    suspend fun clearCall(calleeId: String, callId: String)

    fun observeCallStatus(calleeId: String, callId: String): Flow<CallStatus?>

    suspend fun ackRinging(callerId: String, callId: String, calleeId: String)

    fun observeRingingAck(callerId: String, callId: String): Flow<Boolean>

    suspend fun clearRingingAck(callerId: String, callId: String)

    suspend fun joinCallPresence(callId: String, uid: String)

    fun leaveCallPresence(callId: String, uid: String)

    fun observeCallMembers(callId: String): Flow<Set<String>>
}
