package com.example.messenger.presentation.components.common.model

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

object MotionSprings {
    val press: SpringSpec<Float> = spring(dampingRatio = 0.6f, stiffness = 900f)
    val settle: SpringSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 420f)
    val pop: SpringSpec<Float> = spring(dampingRatio = 0.55f, stiffness = 520f)
}
