package com.example.messenger.presentation.components.call

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.data.remote.call.ActiveCallHolder
import com.example.messenger.data.remote.call.CallUids
import com.example.messenger.di.UiEntryPoints
import com.example.messenger.domain.service.ICallService
import com.example.messenger.presentation.components.call.model.CallBarMode
import com.example.messenger.presentation.components.call.model.SurfaceKind
import com.example.messenger.presentation.components.call.model.VideoStream
import com.example.messenger.presentation.components.common.MessengerAvatar
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val BubbleSize = 60.dp
private val PillWidth = 210.dp
private val VideoTileWidth = 110.dp
private val VideoTileHeight = 160.dp
private val EdgeMargin = 16.dp

@Composable
fun CallBarOverlay(
    onOpenCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active by ActiveCallHolder.state.collectAsStateWithLifecycle()
    val call = active ?: return
    if (!isCallBarVisible(call)) return
    val mode by CallBarPresenter.mode.collectAsStateWithLifecycle()
    if (mode == CallBarMode.BAR) return

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val overlayWidthPx = with(density) { maxWidth.toPx() }
        val overlayHeightPx = with(density) { maxHeight.toPx() }
        val topInsetPx = WindowInsets.statusBars.getTop(density).toFloat()

        when (mode) {
            CallBarMode.HIDDEN -> HiddenPeek(
                topInsetPx = topInsetPx,
                onRestore = { CallBarPresenter.restore() },
            )
            CallBarMode.BUBBLE, CallBarMode.VIDEO -> FloatingCallTile(
                call = call,
                videoForm = mode == CallBarMode.VIDEO,
                overlayWidthPx = overlayWidthPx,
                overlayHeightPx = overlayHeightPx,
                topInsetPx = topInsetPx,
                onOpenCall = onOpenCall,
            )
            CallBarMode.BAR -> Unit
        }
    }
}

@Composable
private fun BoxScope.HiddenPeek(
    topInsetPx: Float,
    onRestore: () -> Unit,
) {
    val density = LocalDensity.current
    val topPadding = with(density) { topInsetPx.toDp() } + 6.dp
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = topPadding)
            .size(width = 48.dp, height = 22.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(BarBackground)
            .clickable(onClick = onRestore)
            .pointerInput(Unit) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { change, delta ->
                        change.consume()
                        dragged += delta
                    },
                    onDragEnd = {
                        if (dragged > 12f) onRestore()
                        dragged = 0f
                    },
                    onDragCancel = { dragged = 0f },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = "Show call bar",
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun BoxScope.FloatingCallTile(
    call: ActiveCallHolder.ActiveCall,
    videoForm: Boolean,
    overlayWidthPx: Float,
    overlayHeightPx: Float,
    topInsetPx: Float,
    onOpenCall: () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val tileWidth = if (videoForm) VideoTileWidth else BubbleSize
    val tileHeight = if (videoForm) VideoTileHeight else BubbleSize
    val animWidth by animateDpAsState(
        targetValue = tileWidth,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "tileWidth",
    )
    val animHeight by animateDpAsState(
        targetValue = tileHeight,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "tileHeight",
    )
    val animCorner by animateDpAsState(
        targetValue = if (videoForm) 16.dp else BubbleSize / 2,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "tileCorner",
    )

    val tileWidthPx = with(density) { tileWidth.toPx() }
    val tileHeightPx = with(density) { tileHeight.toPx() }
    val pillWidthPx = with(density) { PillWidth.toPx() }
    val marginPx = with(density) { EdgeMargin.toPx() }
    val homeTopPx = topInsetPx + with(density) { 68.dp.toPx() }
    val homeHeightPx = with(density) { 56.dp.toPx() }
    val homeTolerancePx = with(density) { 30.dp.toPx() }
    val flingThresholdPx = with(density) { 300.dp.toPx() }

    val defaultOffset = remember(overlayWidthPx, topInsetPx) {
        Offset(
            x = overlayWidthPx - with(density) { BubbleSize.toPx() } - marginPx,
            y = homeTopPx + homeHeightPx + with(density) { 24.dp.toPx() },
        )
    }
    val offset = remember {
        Animatable(CallBarPresenter.bubbleOffset ?: defaultOffset, Offset.VectorConverter)
    }
    val velocityTracker = remember { VelocityTracker() }
    var overHome by remember { mutableStateOf(false) }
    var expandedInfo by remember { mutableStateOf(false) }
    val expand by animateFloatAsState(
        targetValue = if (expandedInfo && !videoForm) 1f else 0f,
        animationSpec = tween(220),
        label = "expand",
    )
    LaunchedEffect(expandedInfo) {
        if (expandedInfo) {
            kotlinx.coroutines.delay(2600)
            expandedInfo = false
        }
    }
    LaunchedEffect(videoForm) {
        if (videoForm) expandedInfo = false
    }

    fun centerY(top: Float) = top + tileHeightPx / 2f

    fun isOverHome(current: Offset): Boolean {
        val cy = centerY(current.y)
        return cy >= homeTopPx - homeTolerancePx && cy <= homeTopPx + homeHeightPx + homeTolerancePx
    }

    fun clampToBounds(raw: Offset): Offset = Offset(
        x = raw.x.coerceIn(marginPx, (overlayWidthPx - tileWidthPx - marginPx).coerceAtLeast(marginPx)),
        y = raw.y.coerceIn(topInsetPx, (overlayHeightPx - tileHeightPx - marginPx).coerceAtLeast(topInsetPx)),
    )

    fun wallTarget(current: Offset, velocityX: Float): Offset {
        val leftX = marginPx
        val rightX = (overlayWidthPx - tileWidthPx - marginPx).coerceAtLeast(marginPx)
        val projectedCenter = current.x + tileWidthPx / 2f + velocityX * 0.10f
        val goRight = if (abs(velocityX) > flingThresholdPx) {
            velocityX > 0f
        } else {
            projectedCenter > overlayWidthPx / 2f
        }
        return Offset(if (goRight) rightX else leftX, current.y)
    }

    LaunchedEffect(videoForm, overlayWidthPx, overlayHeightPx) {
        val clamped = clampToBounds(offset.value)
        if (clamped != offset.value) {
            CallBarPresenter.bubbleOffset = clamped
            offset.animateTo(clamped, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    if (overHome) {
        SnapPreview(topInsetPx = topInsetPx)
    }

    val homeSnapTarget = remember(overlayWidthPx, tileWidthPx) {
        Offset(
            x = (overlayWidthPx - tileWidthPx) / 2f,
            y = homeTopPx,
        )
    }

    Box(
        modifier = Modifier
            .offset {
                val onRightHalf = offset.value.x + tileWidthPx / 2f > overlayWidthPx / 2f
                val renderX = if (onRightHalf) {
                    offset.value.x - (pillWidthPx - tileWidthPx) * expand
                } else {
                    offset.value.x
                }
                IntOffset(renderX.roundToInt(), offset.value.y.roundToInt())
            }
            .size(width = lerp(animWidth, PillWidth, expand), height = animHeight)
            .graphicsLayer {
                val scale = 1f + 0.02f * expand.coerceIn(0f, 1f)
                scaleX = scale
                scaleY = scale
            }
            .shadow(if (videoForm) 8.dp else 0.dp, RoundedCornerShape(animCorner))
            .clip(RoundedCornerShape(animCorner))
            .background(if (videoForm) Color.Black else BarBackground)
            .pointerInput(overlayWidthPx, overlayHeightPx, videoForm) {
                detectDragGestures(
                    onDragStart = {
                        expandedInfo = false
                        overHome = isOverHome(offset.value)
                        velocityTracker.resetTracking()
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        val next = clampToBounds(offset.value + delta)
                        velocityTracker.addPosition(change.uptimeMillis, next)
                        overHome = isOverHome(next)
                        scope.launch { offset.snapTo(next) }
                    },
                    onDragEnd = {
                        val velocity = velocityTracker.calculateVelocity()
                        if (isOverHome(offset.value)) {
                            scope.launch {
                                offset.animateTo(homeSnapTarget, tween(200))
                                overHome = false
                                CallBarPresenter.bubbleOffset = null
                                CallBarPresenter.showBar()
                            }
                        } else {
                            overHome = false
                            val settledY = (offset.value.y + velocity.y * 0.08f)
                                .coerceIn(
                                    topInsetPx,
                                    (overlayHeightPx - tileHeightPx - marginPx).coerceAtLeast(topInsetPx),
                                )
                            val target = wallTarget(offset.value, velocity.x).copy(y = settledY)
                            CallBarPresenter.bubbleOffset = target
                            scope.launch {
                                offset.animateTo(
                                    targetValue = target,
                                    animationSpec = spring(
                                        dampingRatio = 0.72f,
                                        stiffness = Spring.StiffnessLow,
                                    ),
                                    initialVelocity = Offset(velocity.x, velocity.y),
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        overHome = false
                        CallBarPresenter.bubbleOffset = offset.value
                    },
                )
            }
            .pointerInput(videoForm) {
                detectTapGestures(
                    onTap = {
                        if (videoForm || expandedInfo) {
                            expandedInfo = false
                            CallBarPresenter.bubbleOffset = null
                            onOpenCall()
                            CallBarPresenter.showBar()
                        } else {
                            expandedInfo = true
                        }
                    },
                )
            },
        contentAlignment = if (videoForm) Alignment.Center else Alignment.CenterStart,
    ) {
        Crossfade(targetState = videoForm, animationSpec = tween(200), label = "tileForm") { video ->
            if (video) {
                VideoTileContent(call = call)
            } else {
                BubbleContent(call = call, expand = expand)
            }
        }
    }
}

@Composable
private fun VideoTileContent(call: ActiveCallHolder.ActiveCall) {
    if (LocalInspectionMode.current) return
    val appContext = LocalContext.current.applicationContext
    val service = remember(appContext) {
        EntryPointAccessors.fromApplication(appContext, UiEntryPoints::class.java).callService()
    }

    val stream = remember(call.remoteVideoOn, call.wasIncoming, call.callerId, call.calleeId) {
        val partnerId = if (call.wasIncoming) call.callerId else call.calleeId
        if (call.remoteVideoOn && partnerId.isNotBlank()) {
            VideoStream.Remote(CallUids.fromUserId(partnerId))
        } else {
            VideoStream.Local
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        key(stream) {
            CallVideoView(
                host = VideoSurfaceOwner.Host.FLOATING_TILE,
                stream = stream,
                service = service,
                kind = SurfaceKind.TEXTURE,
                modifier = Modifier.fillMaxSize(),
            )
        }
        TileHideButton(
            onClick = { CallBarPresenter.hide() },
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
        )
    }
}

@Composable
private fun TileHideButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Hide call tile",
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun BubbleContent(
    call: ActiveCallHolder.ActiveCall,
    expand: Float,
) {
    val partner = rememberCallPartnerUi(call)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MessengerAvatar(
            name = partner.displayName,
            photoUrl = partner.avatarUrl,
            size = 40.dp,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(2.dp, RingGreen, CircleShape),
        )
        if (expand > 0.15f) {
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { alpha = ((expand - 0.15f) / 0.85f).coerceIn(0f, 1f) },
            ) {
                Text(
                    text = partner.displayName.ifBlank { "On call" },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (isCallConnected(call)) formatHms(call.seconds) else callBarStatus(call),
                    color = RingGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.SnapPreview(topInsetPx: Float) {
    val density = LocalDensity.current
    val topPadding = with(density) { topInsetPx.toDp() } + 68.dp
    val alpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(120), label = "snap")
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = topPadding)
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(56.dp)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(30.dp))
            .background(RingGreen.copy(alpha = 0.14f))
            .border(2.dp, RingGreen.copy(alpha = 0.9f), RoundedCornerShape(30.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Release to dock",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
