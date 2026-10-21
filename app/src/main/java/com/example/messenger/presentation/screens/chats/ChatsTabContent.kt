package com.example.messenger.presentation.screens.chats

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.messenger.domain.model.Conversation
import com.example.messenger.presentation.theme.MessengerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatsTabContent(
    uiState: ConversationsUiState,
    topInset: Dp,
    onRefresh: () -> Unit,
    onChatClick: (String, String, String, String?) -> Unit,
    onEditChats: () -> Unit,
    onMarkRead: (String) -> Unit,
    onMarkUnread: (String) -> Unit,
    onDeleteForMe: (String) -> Unit,
    onDeleteForEveryone: (String) -> Unit,
) {
    when {
        uiState.isLoading -> Box(Modifier.padding(top = topInset)) { ChatsLoading() }
        uiState.error != null -> Box(Modifier.padding(top = topInset)) {
            ChatsError(message = uiState.error.asString(), onRetry = onRefresh)
        }
        uiState.conversations.isEmpty() -> Box(Modifier.padding(top = topInset)) { ChatsEmpty() }
        else -> ChatsList(
            uiState = uiState,
            topInset = topInset,
            onRefresh = onRefresh,
            onChatClick = onChatClick,
            onEditChats = onEditChats,
            onMarkRead = onMarkRead,
            onMarkUnread = onMarkUnread,
            onDeleteForMe = onDeleteForMe,
            onDeleteForEveryone = onDeleteForEveryone,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ConversationsListPreview() {
    MessengerTheme {
        ChatsTabContent(
            uiState = ConversationsUiState(
                conversations = listOf(
                    Conversation(
                        id = "conv1",
                        participantIds = listOf("uid1", "uid2"),
                        participantNames = listOf("Alice"),
                        lastMessage = "Hey, how are you?",
                        lastMessageTimestamp = 1_700_000_000_000L,
                        unreadCount = 2,
                    ),
                    Conversation(
                        id = "conv2",
                        participantIds = listOf("uid1", "uid3"),
                        participantNames = listOf("Bob"),
                        lastMessage = "Let's catch up soon!",
                        lastMessageTimestamp = 1_700_000_060_000L,
                        unreadCount = 0,
                    ),
                ),
                isLoading = false,
            ),
            onRefresh = {},
            onChatClick = { _, _, _, _ -> },
            onEditChats = {},
            onMarkRead = {},
            onMarkUnread = {},
            onDeleteForMe = {},
            onDeleteForEveryone = {},
            topInset = 0.dp,
        )
    }
}
