package com.example.messenger.data.service

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.messenger.data.media.ClipPlaybackController
import com.example.messenger.data.media.MediaCache
import com.example.messenger.data.media.VideoNoteRecorder
import com.example.messenger.data.media.VoiceDraftStore
import com.example.messenger.data.media.VoiceRecorder
import com.example.messenger.data.remote.firebase.FirestoreService
import com.example.messenger.domain.service.ISessionCleaner
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSessionCleaner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestoreService: FirestoreService,
    private val playbackController: Lazy<ClipPlaybackController>,
    private val voiceRecorder: Lazy<VoiceRecorder>,
    private val videoNoteRecorder: Lazy<VideoNoteRecorder>,
    private val voiceDrafts: Lazy<VoiceDraftStore>,
) : ISessionCleaner {

    override suspend fun clearOnLogout(uid: String?) {
        withContext(Dispatchers.Main.immediate) {
            playbackController.get().stop()
            voiceRecorder.get().cancel()
            videoNoteRecorder.get().cancel()
        }
        voiceDrafts.get().clear()

        if (uid != null) {
            firestoreService.deleteFcmToken(uid)
        }
        runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
            .onFailure { Log.w(TAG, "deleteToken failed", it) }

        runCatching { NotificationManagerCompat.from(context).cancelAll() }
            .onFailure { Log.w(TAG, "cancelAll notifications failed", it) }

        wipe(File(context.cacheDir, MediaCache.DIR))
        wipe(File(context.filesDir, MediaCache.OUTBOX_DIR))
        wipe(File(context.filesDir, MediaCache.DRAFTS_DIR))
        wipe(File(context.filesDir, COIL_DIR))
        Log.d(TAG, "session cleared (uid=$uid)")
    }

    override suspend fun clearOnDelete(uid: String?) {
        clearOnLogout(uid)
        if (uid != null) {
            wipe(File(File(context.filesDir, E2EE_DIR), identityKeyFile(uid)))
        }
        Log.d(TAG, "device data wiped (uid=$uid)")
    }

    private fun wipe(target: File) {
        runCatching { if (target.exists()) target.deleteRecursively() }
            .onFailure { Log.w(TAG, "could not wipe ${target.name}", it) }
    }

    private fun identityKeyFile(uid: String) = "identity_v1_$uid.bin"

    private companion object {
        const val TAG = "SessionCleaner"
        const val E2EE_DIR = "e2ee"
        const val COIL_DIR = "image_cache"
    }
}
