package com.example.messenger.presentation.navigation.graph

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.messenger.presentation.navigation.AuthRoute
import com.example.messenger.presentation.screens.auth.AuthScreen
import com.example.messenger.presentation.screens.auth.AuthViewModel

@Composable
internal fun AuthDisplay(authViewModel: AuthViewModel) {
    val backStack = rememberNavBackStack(AuthRoute)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AuthRoute> {
                AuthScreen(
                    viewModel = authViewModel,
                    onAuthSuccess = {},
                )
            }
        },
    )
}
