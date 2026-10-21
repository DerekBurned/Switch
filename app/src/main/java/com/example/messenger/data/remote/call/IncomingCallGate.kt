package com.example.messenger.data.remote.call

import android.util.Log
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.featureflag.FeatureFlags
import com.example.messenger.domain.model.call.CallStatus
import com.example.messenger.domain.service.ICallSignalingService
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class IncomingCallGate @Inject constructor(
    private val featureFlags: FeatureFlags,
    private val signalingService: ICallSignalingService,
) {

    private val startedCallIds = ConcurrentHashMap.newKeySet<String>()

    suspend fun admit(calleeId: String, callId: String): Boolean {
        featureFlags.awaitFresh()
        if (featureFlags.isEnabled(FeatureFlag.CALLS)) return startedCallIds.add(callId)
        Log.w(TAG, "Incoming call $callId declined by feature flag")
        try {
            withTimeoutOrNull(DECLINE_TIMEOUT_MS) {
                signalingService.updateCallStatus(calleeId, callId, CallStatus.DECLINED)
            } ?: Log.w(TAG, "Declining call $callId timed out")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decline call $callId", e)
        }
        return false
    }

    private companion object {
        const val TAG = "IncomingCallGate"
        const val DECLINE_TIMEOUT_MS = 3_000L
    }
}
