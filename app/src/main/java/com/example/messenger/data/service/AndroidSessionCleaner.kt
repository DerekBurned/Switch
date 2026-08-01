package com.example.messenger.data.service

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.messenger.data.remote.firebase.FirestoreService
import com.example.messenger.domain.service.ISessionCleaner
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSessionCleaner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestoreService: FirestoreService,
) : ISessionCleaner {

    override suspend fun clearOnLogout(uid: String?) {
        if (uid != null) {
            firestoreService.deleteFcmToken(uid)
        }
        runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
            .onFailure { Log.w(TAG, "deleteToken failed", it) }

        runCatching { NotificationManagerCompat.from(context).cancelAll() }
            .onFailure { Log.w(TAG, "cancelAll notifications failed", it) }
        Log.d(TAG, "session cleared (uid=$uid)")
    }

    private companion object {
        const val TAG = "SessionCleaner"
    }
}
