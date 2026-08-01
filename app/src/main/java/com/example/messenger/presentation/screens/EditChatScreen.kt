package com.example.messenger.presentation.screens
import com.example.messenger.presentation.components.list.EditChatRow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.presentation.components.call.CallAwareTopBar
import com.example.messenger.domain.model.Conversation
import com.example.messenger.presentation.base.ObserveAsEvents
import com.example.messenger.presentation.components.common.NavHeaderPill
import com.example.messenger.presentation.components.common.PillButton
import com.example.messenger.presentation.components.common.PillButtonStyle
import com.example.messenger.presentation.components.common.WallpaperBackground
import com.example.messenger.presentation.effect.EditChatEffect
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import com.example.messenger.presentation.state.EditChatUiState
import com.example.messenger.presentation.viewmodel.EditChatViewModel

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

@Composable
private fun EditChatScreenContent(
    state: EditChatUiState,
    onBackClick: () -> Unit = {},
    onToggle: (String) -> Unit = {},
    onDelete: () -> Unit = {},
    onMarkAllRead: () -> Unit = {},
) {
    val tokens = messengerTokens
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        CallAwareTopBar {
            NavHeaderPill(title = "Select chat", onBack = onBackClick)
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.conversations, key = { it.id }) { conversation ->
                val selected = conversation.id in state.selectedIds
                EditChatRow(
                    conversation = conversation,
                    selected = selected,
                    onClick = { onToggle(conversation.id) },
                )
                HorizontalDivider(color = tokens.divider, thickness = 0.5.dp)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        ) {
            PillButton(
                text = "Read all",
                onClick = onMarkAllRead,
                style = PillButtonStyle.Neutral,
                modifier = Modifier.weight(1f),
            )
            PillButton(
                text = "Delete",
                onClick = onDelete,
                style = PillButtonStyle.Danger,
                enabled = state.selectedIds.isNotEmpty(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EditChatScreenPreview() {
    MessengerTheme {
        WallpaperBackground {
            EditChatScreenContent(
                state = EditChatUiState(
                    conversations = listOf(
                        Conversation(id = "1", participantNames = listOf("Alice")),
                        Conversation(id = "2", participantNames = listOf("Bob")),
                        Conversation(id = "3", participantNames = listOf("Carol")),
                    ),
                    selectedIds = setOf("1", "3"),
                ),
            )
        }
    }
}
