package com.example.messenger.presentation.components.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

object MotionSprings {
    val press: SpringSpec<Float> = spring(dampingRatio = 0.6f, stiffness = 900f)
    val settle: SpringSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 420f)
    val pop: SpringSpec<Float> = spring(dampingRatio = 0.55f, stiffness = 520f)
}

fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.95f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = MotionSprings.press,
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

fun Modifier.springEnter(
    initialScale: Float = 0.94f,
): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, MotionSprings.settle)
    }
    graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        val scale = initialScale + (1f - initialScale) * p
        scaleX = scale
        scaleY = scale
    }
}
