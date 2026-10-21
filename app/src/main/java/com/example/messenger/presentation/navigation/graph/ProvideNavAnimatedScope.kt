package com.example.messenger.presentation.navigation.graph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.example.messenger.presentation.components.common.LocalNavAnimatedVisibilityScope

@Composable
internal fun ProvideNavAnimatedScope(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNavAnimatedVisibilityScope provides LocalNavAnimatedContentScope.current,
    ) {
        content()
    }
}
