package com.example.messenger.presentation.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.presentation.screens.profile.ProfileViewModel

@Composable
fun SettingsScreen(
    viewModel: ProfileViewModel = hiltViewModel<ProfileViewModel>(),
    onProfileClick: () -> Unit = {},
    onSwitchAccountClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onAppearanceClick: () -> Unit = {},
    onSecurityClick: () -> Unit = {},
    onDataStorageClick: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreenContent(
        user = uiState.user,
        photoUrl = uiState.photos.firstOrNull() ?: uiState.user?.avatarUrl,
        onProfileClick = onProfileClick,
        onSwitchAccountClick = onSwitchAccountClick,
        onPrivacyClick = onPrivacyClick,
        onNotificationsClick = onNotificationsClick,
        onAppearanceClick = onAppearanceClick,
        onSecurityClick = onSecurityClick,
        onDataStorageClick = onDataStorageClick,
        onLanguageClick = onLanguageClick,
        onLogoutClick = onLogoutClick,
    )
}
