package com.example.messenger.presentation.components.chat.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect

class MessageContextMenuItem(
    val key: Any,
    val sourceBounds: Rect,
    val isMe: Boolean,
    val actions: List<MessageAction>,
    val content: @Composable () -> Unit,
)
