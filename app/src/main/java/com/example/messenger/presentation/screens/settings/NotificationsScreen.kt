package com.example.messenger.presentation.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.messenger.presentation.components.common.CardDivider
import com.example.messenger.presentation.components.common.RoundedCard
import com.example.messenger.presentation.components.common.ToggleRow

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    var showNotifications by rememberSaveable { mutableStateOf(true) }
    var sound by rememberSaveable { mutableStateOf(true) }
    var vibration by rememberSaveable { mutableStateOf(true) }
    var messagePreview by rememberSaveable { mutableStateOf(true) }

    SettingsSubScaffold(title = "Notifications & Sounds", onBack = onBack) {
        RoundedCard {
            ToggleRow(
                title = "Show notifications",
                checked = showNotifications,
                onCheckedChange = { showNotifications = it },
            )
            CardDivider()
            ToggleRow(title = "Sound", checked = sound, onCheckedChange = { sound = it })
            CardDivider()
            ToggleRow(title = "Vibration", checked = vibration, onCheckedChange = { vibration = it })
            CardDivider()
            ToggleRow(
                title = "Message preview",
                checked = messagePreview,
                onCheckedChange = { messagePreview = it },
            )
        }
    }
}
