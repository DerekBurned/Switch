package com.example.messenger.data.remote.firebase

import android.util.Log
import com.example.messenger.BuildConfig
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.featureflag.FeatureFlagState
import com.example.messenger.domain.featureflag.FeatureFlags
import com.google.android.gms.tasks.Task
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class RemoteConfigFeatureFlags @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
    private val crashlytics: FirebaseCrashlytics,
) : FeatureFlags {

    private val offByDefault = FeatureFlag.parseKeys(BuildConfig.FEATURES_OFF_BY_DEFAULT)
    private val _state = MutableStateFlow(FeatureFlagState.defaults(offByDefault))
    override val state: StateFlow<FeatureFlagState> = _state.asStateFlow()
    private val started = AtomicBoolean(false)

    override fun start() {
        if (!started.compareAndSet(false, true)) return
        FeatureFlag.unknownKeys(BuildConfig.FEATURES_OFF_BY_DEFAULT).forEach { key ->
            Log.w(TAG, "Unknown feature flag key in FEATURES_OFF_BY_DEFAULT: $key")
        }
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else FETCH_INTERVAL_SECONDS
            },
        )
        remoteConfig.ensureInitialized().addOnCompleteListener { publish() }
        remoteConfig.fetchAndActivate().addOnCompleteListener { publish() }
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                remoteConfig.activate().addOnCompleteListener { publish() }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.w(TAG, "Realtime config update failed", error)
            }
        })
    }

    override suspend fun awaitFresh() {
        withTimeoutOrNull(INIT_TIMEOUT_MS.milliseconds) { awaitQuietly(remoteConfig.ensureInitialized()) }
        withTimeoutOrNull(FETCH_TIMEOUT_MS.milliseconds) {
            awaitQuietly(remoteConfig.fetch(STALE_AFTER_SECONDS))
            awaitQuietly(remoteConfig.activate())
        }
        publish()
    }

    private suspend fun awaitQuietly(task: Task<*>) {
        try {
            task.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Remote config task failed", e)
        }
    }

    private fun publish() {
        val next = FeatureFlagState.resolve(offByDefault, ::remoteValue)
        _state.value = next
        crashlytics.setCustomKey(FLAGS_OFF_KEY, next.disabledKeys().joinToString(","))
    }

    private fun remoteValue(key: String): Boolean? {
        val value = remoteConfig.getValue(key)
        if (value.source != FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) return null
        return runCatching { value.asBoolean() }.getOrNull()
    }

    private companion object {
        const val TAG = "FeatureFlags"
        const val FLAGS_OFF_KEY = "flags_off"
        const val FETCH_INTERVAL_SECONDS = 900L
        const val STALE_AFTER_SECONDS = 300L
        const val INIT_TIMEOUT_MS = 1_000L
        const val FETCH_TIMEOUT_MS = 1_500L
    }
}
