package com.example.messenger.presentation

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.messenger.presentation.components.call.PipCallSurface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.messenger.data.local.prefs.ThemePreferenceStore
import com.example.messenger.data.presence.PresenceManager
import com.example.messenger.data.remote.call.ActiveCallHolder
import com.example.messenger.data.remote.call.CallForegroundService
import com.example.messenger.presentation.navigation.AppNavigation
import com.example.messenger.presentation.navigation.CallRoute
import com.example.messenger.presentation.navigation.ChatRoute
import com.example.messenger.presentation.screens.ui.theme.AppFont
import com.example.messenger.presentation.screens.ui.theme.AppTextSize
import com.example.messenger.presentation.screens.ui.theme.BarPosition
import com.example.messenger.presentation.screens.ui.theme.LocalTabBarPosition
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme
import com.example.messenger.presentation.screens.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var presenceManager: PresenceManager

    @Inject
    lateinit var themePreferenceStore: ThemePreferenceStore

    @Inject
    lateinit var callService: com.example.messenger.domain.service.ICallService

    private val idleScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var idleJob: Job? = null
    private var isIdle = false

    private val pendingIntent = mutableStateOf<Intent?>(null)

    private val inPipMode = mutableStateOf(false)
    private var pipWanted = false
    private var pipVideo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingIntent.value = intent
        setContent {
            val themeMode by themePreferenceStore.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val textSize by themePreferenceStore.textSize
                .collectAsStateWithLifecycle(initialValue = AppTextSize.MEDIUM)
            val appFont by themePreferenceStore.appFont
                .collectAsStateWithLifecycle(initialValue = AppFont.DEFAULT)
            val barPosition by themePreferenceStore.barPosition
                .collectAsStateWithLifecycle(initialValue = BarPosition.TOP)
            MessengerTheme(themeMode = themeMode, textSize = textSize, appFont = appFont) {
              CompositionLocalProvider(LocalTabBarPosition provides barPosition) {
                val deepLink = remember { pendingIntent }
                val pendingRoute = remember { mutableStateOf<NavKey?>(null) }
                LaunchedEffect(deepLink.value) {
                    deepLink.value?.let { pendingRoute.value = resolveDeepLinkRoute(it) }
                    deepLink.value = null
                }
                val activeCall by ActiveCallHolder.state.collectAsStateWithLifecycle()
                LaunchedEffect(
                    activeCall == null,
                    activeCall?.callEnded,
                    activeCall?.localVideoOn,
                    activeCall?.remoteVideoOn,
                ) {
                    val call = activeCall
                    updateCallPipParams(
                        wanted = call != null && !call.callEnded,
                        video = call != null && (call.localVideoOn || call.remoteVideoOn),
                    )
                }
                if (inPipMode.value && activeCall != null) {
                    PipCallSurface(callService)
                } else {
                    AppNavigation(
                        pendingRoute = pendingRoute.value,
                        onRouteConsumed = { pendingRoute.value = null },
                    )
                }
              }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntent.value = intent
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (isIdle) {
            isIdle = false
            presenceManager.goOnline(idleScope)
        }
        scheduleIdleTimer()
    }

    private fun scheduleIdleTimer() {
        idleJob?.cancel()
        idleJob = idleScope.launch {
            delay(IDLE_TIMEOUT_MS)
            isIdle = true
            presenceManager.goIdleAway(idleScope)
        }
    }

    override fun onResume() {
        super.onResume()
        
        isIdle = false
        scheduleIdleTimer()
    }

    override fun onPause() {
        super.onPause()

        idleJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        idleScope.cancel()
    }

    fun updateCallPipParams(wanted: Boolean, video: Boolean) {
        pipWanted = wanted
        pipVideo = video
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { setPictureInPictureParams(pipParams(wanted, video)) }
        }
    }

    private fun pipParams(autoEnter: Boolean, video: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(if (video) Rational(9, 16) else Rational(1, 1))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter)
        }
        return builder.build()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (pipWanted && !isInPictureInPictureMode) {
            runCatching { enterPictureInPictureMode(pipParams(true, pipVideo)) }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPipMode.value = isInPictureInPictureMode
    }

    private fun resolveDeepLinkRoute(intent: Intent): NavKey? = when {
        intent.getBooleanExtra(EXTRA_OPEN_INCOMING_CALL, false) -> {
            val call = ActiveCallHolder.snapshot()
            if (call == null) {
                null
            } else {
                val accept = intent.getBooleanExtra(EXTRA_ACCEPT_CALL, false)
                intent.removeExtra(EXTRA_OPEN_INCOMING_CALL)
                intent.removeExtra(EXTRA_ACCEPT_CALL)
                if (accept) {
                    runCatching {
                        startService(
                            Intent(this, CallForegroundService::class.java)
                                .setAction(CallForegroundService.ACTION_ACCEPT),
                        )
                    }
                }
                CallRoute(
                    partnerId = call.callerId,
                    partnerName = call.partnerName,
                    partnerPhone = call.partnerPhone,
                )
            }
        }
        intent.hasExtra(EXTRA_OPEN_CONVERSATION_ID) -> {
            val conversationId = intent.getStringExtra(EXTRA_OPEN_CONVERSATION_ID).orEmpty()
            val partnerId = intent.getStringExtra(EXTRA_PARTNER_ID).orEmpty()
            val partnerName = intent.getStringExtra(EXTRA_PARTNER_NAME).orEmpty()
            intent.removeExtra(EXTRA_OPEN_CONVERSATION_ID)
            intent.removeExtra(EXTRA_PARTNER_ID)
            intent.removeExtra(EXTRA_PARTNER_NAME)
            if (conversationId.isNotBlank()) {
                ChatRoute(conversationId, partnerId, partnerName)
            } else {
                null
            }
        }
        else -> null
    }

    companion object {
        const val EXTRA_OPEN_INCOMING_CALL = "extra_open_incoming_call"
        const val EXTRA_ACCEPT_CALL = "extra_accept_call"
        const val EXTRA_OPEN_CONVERSATION_ID = "extra_open_conversation_id"
        const val EXTRA_PARTNER_ID = "extra_partner_id"
        const val EXTRA_PARTNER_NAME = "extra_partner_name"
        private const val IDLE_TIMEOUT_MS = 45_000L
    }
}
