package com.example.messenger

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Build
import android.os.ProfilingManager
import android.os.ProfilingTrigger
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.util.DebugLogger
import coil3.video.VideoFrameDecoder
import com.example.messenger.data.crypto.E2eeBootstrap
import com.example.messenger.data.presence.AppLifecycleObserver
import com.example.messenger.data.remote.call.IncomingCallCoordinator
import com.example.messenger.data.remote.call.telecom.TelecomCallManager
import com.example.messenger.data.remote.firebase.FcmTokenSyncer
import com.example.messenger.data.sync.SyncCoordinator
import com.example.messenger.data.sync.SyncManager
import com.example.messenger.presentation.notification.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltAndroidApp
class MessengerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appLifecycleObserver: AppLifecycleObserver

    @Inject
    lateinit var syncCoordinator: SyncCoordinator

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var fcmTokenSyncer: FcmTokenSyncer

    @Inject
    lateinit var incomingCallCoordinator: IncomingCallCoordinator

    @Inject
    lateinit var telecomCallManager: TelecomCallManager

    @Inject
    lateinit var e2eeBootstrap: E2eeBootstrap

    override fun onCreate() {
        super.onCreate()

        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(context, 0.1)
                        .strongReferencesEnabled(true)
                        .build()
                }
                .diskCachePolicy(CachePolicy.ENABLED)
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.filesDir.resolve("image_cache"))
                        .maxSizeBytes(30L * 1024 * 1024)
                        .build()
                }
                .apply {
                    if (com.example.messenger.BuildConfig.DEBUG) {
                        logger(DebugLogger())
                    } else {
                        logger(DebugLogger(coil3.util.Logger.Level.Warn))
                    }
                }
                .components {
                    add(OkHttpNetworkFetcherFactory())
                    add(VideoFrameDecoder.Factory())
                }
                .build()
        }

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("APP_CRASH", "Uncaught exception on thread '${thread.name}'", throwable)
            previous?.uncaughtException(thread, throwable)
        }

        NotificationChannels.ensureCreated(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        syncCoordinator.start()
        syncManager.schedulePeriodicSync()
        fcmTokenSyncer.start()
        incomingCallCoordinator.start()
        telecomCallManager.registerPhoneAccount()
        registerMemoryProfiling()
        e2eeBootstrap.publishKeys()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> Log.d("MEM_TRIM", "BACKGROUND ($level)")
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> Log.d("MEM_TRIM", "UI_HIDDEN ($level)")
        }
    }

    private fun registerMemoryProfiling() {
        if (Build.VERSION.SDK_INT < 37) return
        val profiling = getSystemService(ProfilingManager::class.java) ?: return
        profiling.registerForAllProfilingResults(mainExecutor) { result ->
            Log.i(
                "MEM_PROFILING",
                "trigger=${result.triggerType} error=${result.errorCode} file=${result.resultFilePath}",
            )
        }
        profiling.addProfilingTriggers(
            listOf(
                ProfilingTrigger.Builder(ProfilingTrigger.TRIGGER_TYPE_OOM)
                    .setRateLimitingPeriodHours(24)
                    .build(),
                ProfilingTrigger.Builder(ProfilingTrigger.TRIGGER_TYPE_ANOMALY)
                    .setRateLimitingPeriodHours(24)
                    .build(),
            ),
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
