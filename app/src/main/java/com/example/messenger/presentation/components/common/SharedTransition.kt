package com.example.messenger.presentation.components.common

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import com.example.messenger.presentation.screens.ui.theme.Motion

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@Composable
fun rememberNavEnterSettled(): Boolean {
    val scope = LocalNavAnimatedVisibilityScope.current ?: return true
    return scope.transition.currentState == EnterExitState.Visible
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedElementKey(key: Any): Modifier {
    val transitionScope = LocalSharedTransitionScope.current ?: return this
    val visibilityScope = LocalNavAnimatedVisibilityScope.current ?: return this
    return with(transitionScope) {
        this@sharedElementKey.sharedElement(
            rememberSharedContentState(key = key),
            animatedVisibilityScope = visibilityScope,
            boundsTransform = { _, _ -> tween(Motion.durationMedium, easing = Motion.emphasized) },
        )
    }
}
