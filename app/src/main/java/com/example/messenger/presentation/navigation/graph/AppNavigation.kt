package com.example.messenger.presentation.navigation.graph

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.messenger.domain.model.SessionState
import com.example.messenger.presentation.components.common.WallpaperBackdrop
import com.example.messenger.presentation.notification.NotificationPermissionGate
import com.example.messenger.presentation.screens.auth.AuthViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(
    pendingRoute: NavKey? = null,
    onRouteConsumed: () -> Unit = {},
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Single static wallpaper for the whole app: screens are transparent,
        // so navigation transitions animate only their content over it.
        WallpaperBackdrop()
        when (authState.session) {
            is SessionState.Active -> MainDisplay(
                authViewModel = authViewModel,
                pendingRoute = pendingRoute,
                onRouteConsumed = onRouteConsumed,
            )

            is SessionState.SignedOut -> AuthDisplay(authViewModel = authViewModel)

            SessionState.Resolving -> Unit
        }
        NotificationPermissionGate(
            isAuthenticated = authState.session is SessionState.Active,
        )
    }
}
