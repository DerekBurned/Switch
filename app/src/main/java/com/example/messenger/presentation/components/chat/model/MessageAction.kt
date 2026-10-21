package com.example.messenger.presentation.components.chat.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class MessageAction(
    val title: String,
    val icon: ImageVector,
    val color: Color = Color.Unspecified,
    val onClick: () -> Unit,
)
