package com.example.messenger.presentation.screens.settings

import androidx.compose.runtime.Composable
import com.example.messenger.presentation.components.common.CardDivider
import com.example.messenger.presentation.components.common.RoundedCard
import com.example.messenger.presentation.components.common.SettingsRow

@Composable
fun SecurityScreen(onBack: () -> Unit) {
    SettingsSubScaffold(title = "Security", onBack = onBack) {
        RoundedCard {
            SettingsRow(title = "Two-step verification", onClick = {})
            CardDivider()
            SettingsRow(title = "Passcode lock", onClick = {})
            CardDivider()
            SettingsRow(title = "Active sessions", onClick = {})
        }
    }
}
