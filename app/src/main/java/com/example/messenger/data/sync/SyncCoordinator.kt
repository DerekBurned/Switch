package com.example.messenger.data.sync

import android.util.Log
import com.example.messenger.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncCoordinator @Inject constructor(
    private val syncManager: SyncManager,
    private val networkObserver: INetworkObserver,
) {
    private companion object {
        const val TAG = "SyncCoordinator"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        syncManager.triggerOneTimeSync()
        job = scope.launch {
            networkObserver.isConnected.collectLatest { state ->
                if (state is NetworkUtils.Available) {
                    Log.d(TAG, "Network available — triggering one-time sync")
                    syncManager.triggerOneTimeSync()
                }
            }
        }
    }
}
