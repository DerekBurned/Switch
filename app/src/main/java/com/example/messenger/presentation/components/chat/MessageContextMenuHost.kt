package com.example.messenger.presentation.components.chat

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.messenger.presentation.screens.ui.theme.Motion
import kotlin.math.roundToInt

@Stable
class MessageContextMenuState {
    var active by mutableStateOf<MessageContextMenuItem?>(null)
        private set
    internal var renderingKey by mutableStateOf<Any?>(null)

    fun open(item: MessageContextMenuItem) {
        active = item
    }

    fun dismiss() {
        active = null
    }
}

@Composable
fun rememberMessageContextMenuState(): MessageContextMenuState = remember { MessageContextMenuState() }

class MessageContextMenuItem(
    val key: Any,
    val sourceBounds: Rect,
    val isMe: Boolean,
    val actions: List<MessageAction>,
    val content: @Composable () -> Unit,
)

@Composable
fun LongPressMessage(
    isMe: Boolean,
    messageKey: Any,
    actions: List<MessageAction>,
    state: MessageContextMenuState,
    preview: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val hidden = state.renderingKey == messageKey
    var bounds by remember { mutableStateOf(Rect.Zero) }
    Box(
        modifier = modifier
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .graphicsLayer { alpha = if (hidden) 0f else 1f }
            .pointerInput(messageKey) {
                detectTapGestures(
                    onLongPress = {
                        state.open(MessageContextMenuItem(messageKey, bounds, isMe, actions, preview))
                    },
                )
            },
    ) {
        content()
    }
}

@Composable
fun MessageContextMenuHost(
    state: MessageContextMenuState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var rendered by remember { mutableStateOf<MessageContextMenuItem?>(null) }
    val progress = remember { Animatable(0f) }
    val active = state.active

    LaunchedEffect(active) {
        if (active != null) {
            rendered = active
            state.renderingKey = active.key
            progress.snapTo(0f)
            progress.animateTo(1f, tween(Motion.durationMedium, easing = Motion.emphasized))
        } else if (rendered != null) {
            progress.animateTo(0f, tween(Motion.durationShort, easing = Motion.emphasized))
            rendered = null
            state.renderingKey = null
        }
    }

    var hostOrigin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { hostOrigin = it.positionInRoot() },
    ) {
        val blurEnabled = rendered != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        Box(
            Modifier
                .fillMaxSize()
                .then(if (blurEnabled) Modifier.blur((progress.value * 18f).dp) else Modifier),
        ) {
            content()
        }
        rendered?.let { item ->
            MessageContextMenuOverlay(
                item = item,
                hostOrigin = hostOrigin,
                progress = { progress.value },
                onDismiss = state::dismiss,
            )
        }
    }
}

@Composable
private fun MessageContextMenuOverlay(
    item: MessageContextMenuItem,
    hostOrigin: Offset,
    progress: () -> Float,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val src = item.sourceBounds
        val srcLeft = src.left - hostOrigin.x
        val srcTop = src.top - hostOrigin.y
        val srcRight = src.right - hostOrigin.x
        val gapPx = with(density) { 10.dp.toPx() }
        val topInsetPx = with(density) { 40.dp.toPx() }
        val edgePx = with(density) { 8.dp.toPx() }
        val menuWidthPx = with(density) { 260.dp.toPx() }
        var menuHeightPx by remember(item) {
            mutableStateOf(with(density) { (item.actions.size * 52 + 12).dp.toPx() })
        }

        val groupH = src.height + gapPx + menuHeightPx
        val targetTop = ((screenH - groupH) / 2f).coerceAtLeast(topInsetPx)
        val menuTop = targetTop + src.height + gapPx
        val rawMenuLeft = if (item.isMe) srcRight - menuWidthPx else srcLeft
        val menuLeft = rawMenuLeft.coerceIn(edgePx, (screenW - menuWidthPx - edgePx).coerceAtLeast(edgePx))

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress() * 0.55f }
                .background(Color.Black)
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        )

        Box(
            Modifier
                .offset {
                    IntOffset(
                        x = srcLeft.roundToInt(),
                        y = lerp(srcTop, targetTop, progress()).roundToInt(),
                    )
                }
                .width(with(density) { src.width.toDp() })
                .graphicsLayer {
                    val s = 1f + 0.03f * progress()
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(if (item.isMe) 1f else 0f, 0.5f)
                }
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        ) {
            item.content()
        }

        Box(
            Modifier
                .offset { IntOffset(menuLeft.roundToInt(), menuTop.roundToInt()) }
                .graphicsLayer {
                    val mp = ((progress() - 0.25f) / 0.75f).coerceIn(0f, 1f)
                    alpha = mp
                    scaleY = mp
                    transformOrigin = TransformOrigin(if (item.isMe) 1f else 0f, 0f)
                }
                .onSizeChanged { menuHeightPx = it.height.toFloat() },
        ) {
            ContextMenuContent(
                actions = item.actions.map { action ->
                    action.copy(onClick = {
                        action.onClick()
                        onDismiss()
                    })
                },
            )
        }
    }
}
