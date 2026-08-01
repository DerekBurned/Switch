package com.example.messenger.data.remote.call

import android.content.Context
import androidx.core.content.ContextCompat
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.data.remote.call.telecom.TelecomCallManager
import com.example.messenger.data.remote.call.telecom.TelecomCallMeta
import com.example.messenger.domain.repository.IUserRepository
import com.example.messenger.domain.service.ICallSignalingService
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomingCallCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val signalingService: ICallSignalingService,
    private val firebaseAuthService: FirebaseAuthService,
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: IUserRepository,
    private val telecomCallManager: TelecomCallManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observerJob: Job? = null
    private var lastSeenCallId: String? = null

    fun start() {
        
        firebaseAuth.addAuthStateListener { auth ->
            attachListener(auth.currentUser?.uid)
        }
        attachListener(firebaseAuthService.getCurrentUserId())
    }

    private fun attachListener(uid: String?) {
        observerJob?.cancel()
        lastSeenCallId = null
        if (uid.isNullOrBlank()) return
        observerJob = scope.launch {
            signalingService.observeIncomingCall(uid).collectLatest { signal ->
                if (signal == null) return@collectLatest
                if (signal.callId == lastSeenCallId) return@collectLatest
                
                if (ActiveCallHolder.snapshot()?.callId == signal.callId) return@collectLatest
                lastSeenCallId = signal.callId
                val partnerName = resolveCallerName(signal.callerId)
                val intent = CallForegroundService.incomingIntent(
                    ctx = context,
                    callId = signal.callId,
                    callerId = signal.callerId,
                    calleeId = signal.calleeId,
                    channelName = signal.channelName,
                    partnerName = partnerName,
                    partnerPhone = "",
                    video = signal.video,
                )
                ContextCompat.startForegroundService(context, intent)
                val meta = TelecomCallMeta(
                    callId = signal.callId,
                    callerId = signal.callerId,
                    calleeId = signal.calleeId,
                    channelName = signal.channelName,
                    partnerName = partnerName,
                    partnerPhone = "",
                    isIncoming = true,
                    isVideo = signal.video,
                )
                runCatching { telecomCallManager.addIncoming(meta) }
            }
        }
    }

    private suspend fun resolveCallerName(callerId: String): String {
        val username = withTimeoutOrNull(CALLER_NAME_TIMEOUT_MS) {
            runCatching { userRepository.getUserById(callerId).getOrNull()?.username }.getOrNull()
        }
        return username?.takeIf { it.isNotBlank() } ?: "Unknown caller"
    }

    private companion object {
        const val CALLER_NAME_TIMEOUT_MS = 1500L
    }
}
