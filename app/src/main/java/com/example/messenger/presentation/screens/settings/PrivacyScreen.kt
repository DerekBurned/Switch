package com.example.messenger.presentation.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.presentation.components.common.CardDivider
import com.example.messenger.presentation.components.common.RoundedCard
import com.example.messenger.presentation.components.common.SectionHeader
import com.example.messenger.presentation.components.common.SettingsRow
import com.example.messenger.presentation.components.common.ToggleRow
import com.example.messenger.presentation.featureflag.featureEnabled

@Composable
fun PrivacyScreen(onBack: () -> Unit, onDeleteAccount: () -> Unit = {}) {
    var showLastSeen by rememberSaveable { mutableStateOf(true) }
    var readReceipts by rememberSaveable { mutableStateOf(true) }

    SettingsSubScaffold(title = "Privacy", onBack = onBack) {
        if (featureEnabled(FeatureFlag.SETTINGS_PRIVACY_CONTROLS)) {
            SectionHeader(text = "Visibility")
            RoundedCard {
                ToggleRow(
                    title = "Show \"Last Seen\" & Online",
                    checked = showLastSeen,
                    onCheckedChange = { showLastSeen = it },
                )
                CardDivider()
                ToggleRow(
                    title = "Read receipts",
                    checked = readReceipts,
                    onCheckedChange = { readReceipts = it },
                )
            }
            SectionHeader(text = "Who can…")
            RoundedCard {
                SettingsRow(title = "See my profile photo", onClick = {})
                CardDivider()
                SettingsRow(title = "Call me", onClick = {})
                CardDivider()
                SettingsRow(title = "See my phone number", onClick = {})
                CardDivider()
                SettingsRow(title = "Add me to groups", onClick = {})
            }
        }
        SectionHeader(text = "Danger zone")
        RoundedCard {
            SettingsRow(title = "Delete account", onClick = onDeleteAccount)
        }
    }
}
