package com.example.messenger.presentation.screens.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun MessageAppearance(
    messageId: String,
    isMe: Boolean,
    exiting: Boolean,
    animatedIds: MutableSet<String>,
    content: @Composable () -> Unit,
) {
    val isNew = remember(messageId) { messageId !in animatedIds }
    val progress = remember(messageId) { Animatable(if (isNew) 0f else 1f) }
    val slidePx = with(LocalDensity.current) { 120.dp.toPx() }

    LaunchedEffect(messageId) {
        if (isNew) {
            animatedIds.add(messageId)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
    }
    LaunchedEffect(exiting) {
        if (exiting) {
            progress.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 200))
        }
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                val p = progress.value
                alpha = p.coerceIn(0f, 1f)
                if (exiting) {
                    transformOrigin = TransformOrigin(if (isMe) 1f else 0f, 0f)
                    scaleX = p
                    scaleY = p
                } else {
                    translationX = (1f - p) * slidePx * if (isMe) 1f else -1f
                    val s = 0.7f + 0.3f * p
                    scaleX = s
                    scaleY = s
                }
            }
            .then(
                if (exiting) {
                    Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val h = (placeable.height * progress.value).roundToInt().coerceAtLeast(0)
                        layout(placeable.width, h) { placeable.place(0, 0) }
                    }
                } else {
                    Modifier
                }
            ),
    ) {
        content()
    }
}
