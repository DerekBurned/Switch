package com.example.messenger.presentation

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.messenger.R
import com.example.messenger.data.local.prefs.ThemePreferenceStore
import com.example.messenger.data.presence.PresenceManager
import com.example.messenger.data.remote.call.ActiveCallHolder
import com.example.messenger.data.remote.call.CallForegroundService
import com.example.messenger.data.remote.call.CallUids
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.featureflag.FeatureFlags
import com.example.messenger.presentation.components.call.PipCallSurface
import com.example.messenger.presentation.components.call.PipSourceRect
import com.example.messenger.presentation.featureflag.LocalFeatureFlags
import com.example.messenger.presentation.navigation.CallRoute
import com.example.messenger.presentation.navigation.ChatRoute
import com.example.messenger.presentation.navigation.graph.AppNavigation
import com.example.messenger.presentation.theme.LocalTabBarPosition
import com.example.messenger.presentation.theme.MessengerTheme
import com.example.messenger.presentation.theme.ThemeMode
import com.example.messenger.presentation.theme.model.AppFont
import com.example.messenger.presentation.theme.model.AppTextSize
import com.example.messenger.presentation.theme.model.BarPosition
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

    @Inject
    lateinit var featureFlags: FeatureFlags

    private val idleScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var idleJob: Job? = null
    private var isIdle = false

    private val pendingIntent = mutableStateOf<Intent?>(null)

    private val inPipMode = mutableStateOf(false)
    private var pipWanted = false
    private var pipVideo = false
    private var pipSourceRect: Rect? = null

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
            val flagState by featureFlags.state.collectAsStateWithLifecycle()
            MessengerTheme(themeMode = themeMode, textSize = textSize, appFont = appFont) {
              CompositionLocalProvider(
                  LocalTabBarPosition provides barPosition,
                  LocalFeatureFlags provides flagState,
              ) {
                val deepLink = remember { pendingIntent }
                val pendingRoute = remember { mutableStateOf<NavKey?>(null) }
                LaunchedEffect(deepLink.value) {
                    deepLink.value?.let { pendingRoute.value = resolveDeepLinkRoute(it) }
                    deepLink.value = null
                }
                val activeCall by ActiveCallHolder.state.collectAsStateWithLifecycle()
                val sourceRect by PipSourceRect.rect.collectAsStateWithLifecycle()
                LaunchedEffect(
                    activeCall == null,
                    activeCall?.callEnded,
                    activeCall?.localVideoOn,
                    activeCall?.remoteVideoOn,
                    activeCall?.muted,
                    activeCall?.speakerOn,
                    sourceRect,
                    flagState,
                ) {
                    val call = activeCall
                    if (!inPipMode.value) pipSourceRect = sourceRect
                    updateCallPipParams(
                        wanted = call != null && !call.callEnded,
                        video = call != null && (call.localVideoOn || call.remoteVideoOn),
                    )
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        pendingRoute = pendingRoute.value,
                        onRouteConsumed = { pendingRoute.value = null },
                    )
                    if (inPipMode.value) {
                        PipCallSurface(callService)
                    }
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
        pipWanted = wanted && featureFlags.isEnabled(FeatureFlag.CALLS_PIP)
        pipVideo = video
        runCatching { setPictureInPictureParams(pipParams(pipWanted, video)) }
    }

    private fun pipParams(autoEnter: Boolean, video: Boolean): PictureInPictureParams {
        val call = ActiveCallHolder.snapshot()
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(if (video) Rational(9, 16) else Rational(1, 1))
            .setActions(pipActions(call))
        if (video) pipSourceRect?.let { builder.setSourceRectHint(it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter)
            builder.setSeamlessResizeEnabled(video)
            builder.setTitle(call?.partnerName?.takeIf { it.isNotBlank() } ?: getString(R.string.app_name))
            builder.setSubtitle(if (video) "Video call" else "Voice call")
        }
        return builder.build()
    }

    private fun pipActions(call: ActiveCallHolder.ActiveCall?): List<RemoteAction> {
        if (call == null || call.callEnded) return emptyList()
        val actions = mutableListOf<RemoteAction>()
        actions += pipAction(
            iconRes = if (call.muted) R.drawable.ic_pip_mic_off else R.drawable.ic_pip_mic_on,
            title = if (call.muted) "Unmute" else "Mute",
            action = CallForegroundService.ACTION_TOGGLE_MUTE,
            requestCode = REQ_PIP_MUTE,
        )
        if (call.localVideoOn || featureFlags.isEnabled(FeatureFlag.CALLS_VIDEO)) {
            actions += pipAction(
                iconRes = if (call.localVideoOn) R.drawable.ic_pip_video_on else R.drawable.ic_pip_video_off,
                title = if (call.localVideoOn) "Camera off" else "Camera on",
                action = CallForegroundService.ACTION_TOGGLE_CAMERA,
                requestCode = REQ_PIP_CAMERA,
            )
        }
        if (maxNumPictureInPictureActions >= 4 && featureFlags.isEnabled(FeatureFlag.CALLS_SPEAKER)) {
            actions += pipAction(
                iconRes = R.drawable.ic_pip_speaker,
                title = if (call.speakerOn) "Speaker off" else "Speaker on",
                action = CallForegroundService.ACTION_TOGGLE_SPEAKER,
                requestCode = REQ_PIP_SPEAKER,
            )
        }
        actions += pipAction(
            iconRes = R.drawable.ic_pip_call_end,
            title = "Hang up",
            action = CallForegroundService.ACTION_END,
            requestCode = REQ_PIP_END,
        )
        return actions.take(maxNumPictureInPictureActions.coerceAtLeast(1))
    }

    private fun pipAction(
        iconRes: Int,
        title: String,
        action: String,
        requestCode: Int,
    ): RemoteAction {
        val intent = Intent(this, CallForegroundService::class.java).setAction(action)
        val pendingIntent = PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return RemoteAction(Icon.createWithResource(this, iconRes), title, title, pendingIntent)
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
        applyRemoteVideoQuality(low = isInPictureInPictureMode)
    }

    private fun applyRemoteVideoQuality(low: Boolean) {
        val call = ActiveCallHolder.snapshot() ?: return
        val partnerId = if (call.wasIncoming) call.callerId else call.calleeId
        if (partnerId.isBlank()) return
        runCatching { callService.setRemoteVideoQuality(CallUids.fromUserId(partnerId), low) }
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
        private const val REQ_PIP_MUTE = 5
        private const val REQ_PIP_SPEAKER = 6
        private const val REQ_PIP_CAMERA = 7
        private const val REQ_PIP_END = 8
    }
}
