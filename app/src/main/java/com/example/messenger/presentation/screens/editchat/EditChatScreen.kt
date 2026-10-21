package com.example.messenger.presentation.screens.editchat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.presentation.base.ObserveAsEvents

@Composable
fun EditChatScreen(
    viewModel: EditChatViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.effect) { effect ->
        when (effect) {
            EditChatEffect.Done -> onBackClick()
        }
    }
    EditChatScreenContent(
        state = state,
        onBackClick = onBackClick,
        onToggle = viewModel::toggle,
        onDelete = { viewModel.deleteSelected() },
        onMarkAllRead = { viewModel.markAllRead() },
    )
}
