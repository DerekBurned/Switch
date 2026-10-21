package com.example.messenger.presentation.components.chat

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

internal enum class RecordGestureEnd { SEND, DRAFT, NONE }

internal fun recordGestureEnd(locked: Boolean, slidToCancel: Boolean, released: Boolean): RecordGestureEnd = when {
    locked || slidToCancel -> RecordGestureEnd.NONE
    released -> RecordGestureEnd.SEND
    else -> RecordGestureEnd.DRAFT
}

internal fun dragProgress(distancePx: Float, thresholdPx: Float): Float =
    if (thresholdPx <= 0f) 0f else (distancePx / thresholdPx).coerceIn(0f, 1f)

private enum class EarlyEnd { TAP, LOST }

@Composable
fun Modifier.recordGesture(
    enabled: Boolean,
    onTap: () -> Unit,
    onStart: () -> Unit,
    onLock: () -> Unit,
    onCancel: () -> Unit,
    onFinish: () -> Unit,
    onInterrupt: () -> Unit,
    onDrag: (Offset) -> Unit,
    lockEnabled: Boolean = true,
): Modifier {
    val canLock by rememberUpdatedState(lockEnabled)
    val tap by rememberUpdatedState(onTap)
    val start by rememberUpdatedState(onStart)
    val lock by rememberUpdatedState(onLock)
    val cancel by rememberUpdatedState(onCancel)
    val finish by rememberUpdatedState(onFinish)
    val interrupt by rememberUpdatedState(onInterrupt)
    val drag by rememberUpdatedState(onDrag)
    val density = LocalDensity.current
    val cancelPx = with(density) { CANCEL_THRESHOLD.toPx() }
    val lockPx = with(density) { LOCK_THRESHOLD.toPx() }
    val origin = remember { arrayOf(Offset.Zero) }
    if (!enabled) return this
    return this
        .onGloballyPositioned { coordinates -> origin[0] = coordinates.positionInRoot() }
        .pointerInput(cancelPx, lockPx) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                val downInRoot = down.position + origin[0]
                val early = withTimeoutOrNull(HOLD_TO_RECORD_MS) {
                    var outcome: EarlyEnd? = null
                    while (outcome == null) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                        outcome = when {
                            change == null -> EarlyEnd.LOST
                            !change.pressed -> EarlyEnd.TAP
                            else -> null
                        }
                        change?.consume()
                    }
                    outcome
                }
                when (early) {
                    EarlyEnd.TAP -> {
                        tap()
                        return@awaitEachGesture
                    }
                    EarlyEnd.LOST -> return@awaitEachGesture
                    null -> Unit
                }
                start()
                var cancelled = false
                var locked = false
                var released = false
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            released = !change.isConsumed
                            change.consume()
                            break
                        }
                        change.consume()
                        val delta = change.position + origin[0] - downInRoot
                        drag(delta)
                        if (!locked && delta.x < -cancelPx) {
                            cancelled = true
                            cancel()
                            break
                        }
                        if (canLock && !locked && delta.y < -lockPx) {
                            locked = true
                            lock()
                        }
                    }
                } finally {
                    drag(Offset.Zero)
                    when (recordGestureEnd(locked = locked, slidToCancel = cancelled, released = released)) {
                        RecordGestureEnd.SEND -> finish()
                        RecordGestureEnd.DRAFT -> interrupt()
                        RecordGestureEnd.NONE -> Unit
                    }
                }
            }
        }
}

internal val CANCEL_THRESHOLD = 100.dp
internal val LOCK_THRESHOLD = 80.dp
private const val HOLD_TO_RECORD_MS = 200L
