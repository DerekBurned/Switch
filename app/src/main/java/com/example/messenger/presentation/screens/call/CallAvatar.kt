package com.example.messenger.presentation.screens.call

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.messenger.presentation.components.common.MessengerAvatar

@Composable
internal fun CallAvatar(name: String, photoUrl: String? = null) {
    MessengerAvatar(name = name, photoUrl = photoUrl, size = 121.dp)
}
