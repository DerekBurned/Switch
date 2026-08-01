package com.example.messenger.data.crypto

import com.example.messenger.data.remote.auth.FirebaseAuthService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class E2eeBootstrap @Inject constructor(
    private val authService: FirebaseAuthService,
    private val peerKeyRegistry: PeerKeyRegistry,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun publishKeys() {
        val uid = authService.getCurrentUserId() ?: return
        scope.launch {
            runCatching { peerKeyRegistry.publishIfNeeded(uid) }
        }
    }
}
