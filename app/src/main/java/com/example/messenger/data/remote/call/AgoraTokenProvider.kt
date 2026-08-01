package com.example.messenger.data.remote.call

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class AgoraTokenProvider @Inject constructor(
    private val functions: FirebaseFunctions,
) {

    suspend fun fetchToken(channelName: String): String? {
        return runCatching {
            val result = functions
                .getHttpsCallable("getRtcToken")
                .call(mapOf("channel" to channelName))
                .await()
            (result.data as? Map<*, *>)?.get("token") as? String
        }.getOrElse { e ->
            Log.w(TAG, "RTC token fetch failed; joining without token", e)
            null
        }
    }

    companion object {
        private const val TAG = "AgoraTokenProvider"
    }
}
