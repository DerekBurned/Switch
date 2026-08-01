package com.example.messenger.presentation.components.call

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.data.remote.call.ActiveCallHolder
import com.example.messenger.data.remote.call.CallForegroundService
import com.example.messenger.di.UiEntryPoints
import com.example.messenger.domain.service.CallConnectionState
import com.example.messenger.presentation.components.common.MessengerAvatar
import com.example.messenger.util.resolveDisplayName
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal val BarBackground = Color(0xFF1F2A37)
internal val RingGreen = Color(0xFF34C759)
private val EndRed = Color(0xFFE53935)

val LocalOpenActiveCall = staticCompositionLocalOf<() -> Unit> { {} }

val LocalCallBarInset = androidx.compose.runtime.compositionLocalOf { 0.dp }

internal fun isCallBarVisible(call: ActiveCallHolder.ActiveCall): Boolean =
    call.isActive || (!call.isIncoming && !call.callEnded)

internal fun isCallConnected(call: ActiveCallHolder.ActiveCall): Boolean =
    call.isActive && call.remotePresent && call.connectionState == CallConnectionState.CONNECTED

internal fun callBarStatus(call: ActiveCallHolder.ActiveCall): String = when {
    call.callEnded -> "Call ended"
    call.isActive -> "Connecting…"
    call.remoteRinging -> "Ringing…"
    else -> "Requesting…"
}

@Composable
fun CallAwareTopBar(topBar: @Composable () -> Unit) {
    Column(modifier = Modifier.background(Color.Transparent)) {
        topBar()
        Spacer(modifier = Modifier.height(LocalCallBarInset.current))
    }
}

data class CallPartnerUi(
    val displayName: String,
    val avatarUrl: String?,
)

private object CallPartnerCache {
    val avatars = mutableMapOf<String, String?>()
    val aliases = mutableMapOf<String, String?>()
}

@Composable
internal fun rememberCallPartnerUi(call: ActiveCallHolder.ActiveCall): CallPartnerUi {
    val partnerId = if (call.wasIncoming) call.callerId else call.calleeId
    var avatarUrl by remember(partnerId) { mutableStateOf(CallPartnerCache.avatars[partnerId]) }
    var alias by remember(partnerId) { mutableStateOf(CallPartnerCache.aliases[partnerId]) }
    if (!LocalInspectionMode.current) {
        val appContext = LocalContext.current.applicationContext
        LaunchedEffect(partnerId) {
            if (partnerId.isBlank()) return@LaunchedEffect
            val repo = EntryPointAccessors
                .fromApplication(appContext, UiEntryPoints::class.java)
                .userRepository()
            avatarUrl = runCatching { repo.getUserById(partnerId).getOrNull()?.avatarUrl }
                .onFailure { Log.w("CallBarPartner", "avatar lookup failed for $partnerId", it) }
                .getOrNull()
                ?: avatarUrl
            CallPartnerCache.avatars[partnerId] = avatarUrl
            alias = runCatching {
                withTimeoutOrNull(2_000) { repo.observeContactAliases().first()[partnerId] }
            }.getOrNull() ?: alias
            CallPartnerCache.aliases[partnerId] = alias
        }
    }
    return CallPartnerUi(
        displayName = resolveDisplayName(call.partnerName, alias),
        avatarUrl = avatarUrl,
    )
}

@Composable
fun ActiveCallBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    routeAllowsBar: Boolean = true,
) {
    val active by ActiveCallHolder.state.collectAsStateWithLifecycle()
    val mode by CallBarPresenter.mode.collectAsStateWithLifecycle()
    val call = active
    val visible = routeAllowsBar && call != null && isCallBarVisible(call) && mode == CallBarMode.BAR

    var shown by remember { mutableStateOf<ActiveCallHolder.ActiveCall?>(null) }
    if (call != null && visible) shown = call

    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    val dragY = remember { Animatable(0f) }
    val hideThresholdPx = with(density) { 40.dp.toPx() }
    val bubbleThresholdPx = with(density) { 40.dp.toPx() }
    val hideTargetPx = with(density) { 120.dp.toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val topInsetPx = WindowInsets.statusBars.getTop(density).toFloat()
    val bubbleSpawnX = screenWidthPx - with(density) { (60.dp + 16.dp).toPx() }
    val barHomeTopPx = topInsetPx + with(density) { 68.dp.toPx() }

    LaunchedEffect(visible) {
        if (visible) dragY.snapTo(0f)
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxWidth(),
        enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut(),
    ) {
        val barCall = shown ?: return@AnimatedVisibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = dragY.value }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, delta ->
                            change.consume()
                            scope.launch { dragY.snapTo(dragY.value + delta) }
                        },
                        onDragEnd = {
                            val offset = dragY.value
                            when {
                                offset <= -hideThresholdPx -> scope.launch {
                                    dragY.animateTo(-hideTargetPx, tween(160))
                                    CallBarPresenter.hide()
                                }
                                offset >= bubbleThresholdPx -> {
                                    CallBarPresenter.bubbleOffset = Offset(
                                        x = bubbleSpawnX,
                                        y = barHomeTopPx + dragY.value,
                                    )
                                    CallBarPresenter.minimizeToBubble()
                                }
                                else -> scope.launch { dragY.animateTo(0f, tween(180)) }
                            }
                        },
                        onDragCancel = { scope.launch { dragY.animateTo(0f, tween(180)) } },
                    )
                },
        ) {
            CallBarRow(call = barCall, context = context, onClick = onClick)
        }
    }
}

@Composable
internal fun CallBarRow(
    call: ActiveCallHolder.ActiveCall,
    context: Context,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val partner = rememberCallPartnerUi(call)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 6.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(BarBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            MessengerAvatar(
                name = partner.displayName,
                photoUrl = partner.avatarUrl,
                size = 40.dp,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(2.dp, RingGreen, CircleShape),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = partner.displayName.ifBlank { "On call" },
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCallConnected(call)) {
                    CallWave(active = call.remotePresent)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatHms(call.seconds),
                        color = RingGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                } else {
                    Text(
                        text = callBarStatus(call),
                        color = RingGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        CircleActionButton(
            icon = if (call.muted) Icons.Filled.MicOff else Icons.Filled.Mic,
            background = Color.White.copy(alpha = 0.12f),
            iconTint = Color.White,
            contentDescription = if (call.muted) "Unmute" else "Mute",
        ) { sendCallAction(context, CallForegroundService.ACTION_TOGGLE_MUTE) }
        Spacer(Modifier.width(10.dp))
        CircleActionButton(
            icon = Icons.Filled.CallEnd,
            background = EndRed,
            iconTint = Color.White,
            contentDescription = "End call",
        ) { sendCallAction(context, CallForegroundService.ACTION_END) }
    }
}

@Composable
private fun CircleActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    iconTint: Color,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun CallWave(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "wave")
    val heights = (0 until 4).map { index ->
        transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 500 + index * 120),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bar$index",
        )
    }
    Row(
        modifier = Modifier.height(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        heights.forEach { h ->
            val fraction = if (active) h.value else 0.25f
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(RingGreen),
            )
        }
    }
}

internal fun sendCallAction(context: Context, action: String) {
    runCatching {
        context.startService(
            Intent(context, CallForegroundService::class.java).setAction(action),
        )
    }
}

internal fun formatHms(s: Int): String {
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
