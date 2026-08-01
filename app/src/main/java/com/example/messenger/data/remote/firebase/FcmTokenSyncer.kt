package com.example.messenger.data.remote.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenSyncer @Inject constructor(
    private val auth: FirebaseAuth,
    private val messagingManager: FirebaseMessagingManager,
    private val firestoreService: FirestoreService,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastSyncedUid: String? = null

    fun start() {
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                lastSyncedUid = null
                return@addAuthStateListener
            }
            if (uid == lastSyncedUid) return@addAuthStateListener
            syncFor(uid)
        }
    }

    private fun syncFor(uid: String) {
        scope.launch {
            messagingManager.getFcmToken()
                .onSuccess { token ->
                    val result = firestoreService.updateFcmToken(uid, token)
                    if (result.isSuccess) {
                        lastSyncedUid = uid
                        Log.d(TAG, "FCM token synced")
                    } else {
                        Log.w(TAG, "Failed to write FCM token", result.exceptionOrNull())
                    }
                }
                .onFailure { e ->
                    Log.w(TAG, "Failed to fetch FCM token", e)
                }
        }
    }

    private companion object {
        const val TAG = "FcmTokenSyncer"
    }
}
