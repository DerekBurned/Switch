package com.example.messenger.presentation.screens.editchat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.messenger.domain.model.Conversation
import com.example.messenger.presentation.components.call.CallAwareTopBar
import com.example.messenger.presentation.components.common.NavHeaderPill
import com.example.messenger.presentation.components.common.PillButton
import com.example.messenger.presentation.components.common.WallpaperBackground
import com.example.messenger.presentation.components.common.model.PillButtonStyle
import com.example.messenger.presentation.components.list.EditChatRow
import com.example.messenger.presentation.theme.MessengerTheme
import com.example.messenger.presentation.theme.messengerTokens

@Composable
internal fun EditChatScreenContent(
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
private fun EditChatScreenPreview() {
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
