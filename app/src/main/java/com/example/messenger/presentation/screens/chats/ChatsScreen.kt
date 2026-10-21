package com.example.messenger.presentation.screens.chats

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.presentation.components.call.LocalCallBarInset
import com.example.messenger.presentation.components.common.LocalClipBarInset
import com.example.messenger.presentation.theme.LocalTabBarPosition
import com.example.messenger.presentation.theme.model.BarPosition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    viewModel: ChatsViewModel = hiltViewModel(),
    onChatClick: (String, String, String, String?) -> Unit = { _, _, _, _ -> },
    onEditChats: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val barAtBottom = LocalTabBarPosition.current == BarPosition.BOTTOM

    val topInset = (if (barAtBottom) 0.dp else 72.dp) +
        LocalCallBarInset.current + LocalClipBarInset.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        ChatsTabContent(
            uiState = uiState,
            topInset = topInset,
            onRefresh = viewModel::refresh,
            onChatClick = onChatClick,
            onEditChats = onEditChats,
            onMarkRead = viewModel::markAsRead,
            onMarkUnread = viewModel::markAsUnread,
            onDeleteForMe = viewModel::deleteForMe,
            onDeleteForEveryone = viewModel::deleteForEveryone,
        )
    }
}
