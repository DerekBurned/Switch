package com.example.messenger.data.presence

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLifecycleObserver @Inject constructor(
    private val presenceManager: PresenceManager
) : DefaultLifecycleObserver {
    private val APP_LIFECYCLE_OBSERVER = "APP_LIFECYCLE_OBSERVER"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStart(owner: LifecycleOwner) {
        presenceManager.goOnline(scope)
    }

    override fun onPause(owner: LifecycleOwner) {
        presenceManager.goAway(scope)
    }

    override fun onResume(owner: LifecycleOwner) {
        presenceManager.goOnline(scope)
        Log.d(APP_LIFECYCLE_OBSERVER, "")
    }

    override fun onStop(owner: LifecycleOwner) {
        presenceManager.goOffline(scope)
    }
}
