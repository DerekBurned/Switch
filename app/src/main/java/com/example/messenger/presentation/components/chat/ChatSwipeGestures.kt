package com.example.messenger.presentation.components.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

fun Modifier.chatNavigationSwipe(
    enabled: Boolean = true,
    thresholdDp: Dp = 72.dp,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
): Modifier = this.pointerInput(enabled) {
    if (!enabled) return@pointerInput
    val thresholdPx = thresholdDp.toPx()
    var totalDx = 0f
    detectHorizontalDragGestures(
        onDragStart = { totalDx = 0f },
        onDragEnd = {
            when {
                totalDx >= thresholdPx -> onSwipeRight()
                totalDx <= -thresholdPx -> onSwipeLeft()
            }
        },
        onDragCancel = { totalDx = 0f },
    ) { change, dragAmount ->
        totalDx += dragAmount
        change.consume()
    }
}

@Composable
fun SwipeToReply(
    onReply: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val triggerPx = with(density) { 56.dp.toPx() }
    val maxPx = with(density) { 80.dp.toPx() }

    Box(modifier = modifier) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    val progress = (offsetX.value / triggerPx).coerceIn(0f, 1f)
                    alpha = progress
                    val s = 0.6f + 0.4f * progress
                    scaleX = s
                    scaleY = s
                },
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startX = down.position.x
                        val startY = down.position.y
                        var claimed = false
                        var hapticArmed = false
                        var current = 0f
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val dx = change.position.x - startX
                            val dy = change.position.y - startY
                            if (!claimed) {
                                if (dx > slop && dx > abs(dy)) {
                                    claimed = true
                                } else if (dx < -slop || abs(dy) > slop) {
                                    break
                                }
                            }
                            if (claimed && change.positionChanged()) {
                                change.consume()
                                current = (dx * 0.6f).coerceIn(0f, maxPx)
                                scope.launch { offsetX.snapTo(current) }
                                if (!hapticArmed && current >= triggerPx) {
                                    hapticArmed = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                } else if (hapticArmed && current < triggerPx) {
                                    hapticArmed = false
                                }
                            }
                        }
                        if (claimed) {
                            val shouldReply = current >= triggerPx
                            scope.launch { offsetX.animateTo(0f, tween(200)) }
                            if (shouldReply) onReply()
                        }
                    }
                },
        ) {
            content()
        }
    }
}
