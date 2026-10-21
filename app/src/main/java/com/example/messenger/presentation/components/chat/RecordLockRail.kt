package com.example.messenger.presentation.components.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.messenger.R
import com.example.messenger.presentation.theme.messengerTokens

@Composable
fun RecordLockRail(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    val tokens = messengerTokens
    val transition = rememberInfiniteTransition(label = "lockHint")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "lockHintBob",
    )
    val description = stringResource(R.string.voice_slide_up_to_lock)
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .size(width = RECORD_LOCK_RAIL_WIDTH, height = RECORD_LOCK_RAIL_HEIGHT)
            .shadow(elevation = 6.dp, shape = shape, clip = false)
            .clip(shape)
            .background(tokens.backgroundBottom)
            .background(tokens.cardFill)
            .border(1.dp, tokens.panelBorder, shape)
            .semantics { contentDescription = description },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val p = progress().coerceIn(0f, 1f)
            val centerX = size.width / 2f
            val lockY = LOCK_CENTER_Y.toPx()
            val fillTop = size.height + (lockY - size.height) * p
            drawRect(
                color = tokens.accent.copy(alpha = 0.18f),
                topLeft = Offset(0f, fillTop),
                size = Size(size.width, size.height - fillTop),
            )
            if (p > 0f) {
                drawCircle(
                    color = tokens.accent,
                    radius = KNOB_RADIUS.toPx(),
                    center = Offset(centerX, fillTop.coerceAtLeast(lockY + LOCK_CLEARANCE.toPx())),
                )
            }
            val chevronAlpha = (1f - p * 2f).coerceIn(0f, 1f)
            if (chevronAlpha > 0f) {
                drawChevron(
                    center = Offset(centerX, CHEVRON_Y.toPx() - bob * CHEVRON_TRAVEL.toPx()),
                    color = tokens.textMuted.copy(alpha = chevronAlpha),
                )
            }
            drawPadlock(
                center = Offset(centerX, lockY),
                closed = p,
                color = lerp(tokens.textMuted, tokens.accent, p),
            )
        }
    }
}

private fun DrawScope.drawPadlock(center: Offset, closed: Float, color: Color) {
    val bodyWidth = 14.dp.toPx()
    val bodyHeight = 11.dp.toPx()
    val bodyTop = center.y - bodyHeight / 2f + 3.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - bodyWidth / 2f, bodyTop),
        size = Size(bodyWidth, bodyHeight),
        cornerRadius = CornerRadius(2.5.dp.toPx()),
    )
    val stroke = 2.dp.toPx()
    val shackleHalf = 4.5.dp.toPx()
    val lift = (1f - closed) * 5.dp.toPx()
    val legTop = bodyTop - 4.dp.toPx() - lift
    val anchor = bodyTop + stroke / 2f
    val left = center.x - shackleHalf
    val right = center.x + shackleHalf
    val path = Path().apply {
        moveTo(left, anchor)
        lineTo(left, legTop)
        arcTo(
            rect = Rect(left = left, top = legTop - shackleHalf, right = right, bottom = legTop + shackleHalf),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false,
        )
        lineTo(right, anchor - lift)
    }
    drawPath(path = path, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawChevron(center: Offset, color: Color) {
    val half = 5.dp.toPx()
    val rise = 3.dp.toPx()
    val path = Path().apply {
        moveTo(center.x - half, center.y + rise)
        lineTo(center.x, center.y - rise)
        lineTo(center.x + half, center.y + rise)
    }
    drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private val RECORD_LOCK_RAIL_HEIGHT = 132.dp
private val RECORD_LOCK_RAIL_WIDTH = 44.dp
private val LOCK_CENTER_Y = 26.dp
private val LOCK_CLEARANCE = 18.dp
private val CHEVRON_Y = 66.dp
private val CHEVRON_TRAVEL = 6.dp
private val KNOB_RADIUS = 4.dp
