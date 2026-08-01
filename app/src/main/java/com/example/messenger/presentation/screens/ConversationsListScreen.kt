package com.example.messenger.presentation.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import com.example.messenger.presentation.components.call.LocalCallBarInset
import com.example.messenger.presentation.components.common.MessengerAvatar
import com.example.messenger.presentation.components.list.ConversationListItem
import com.example.messenger.presentation.components.list.SwipeableConversationRow
import com.example.messenger.presentation.components.common.sharedElementKey
import com.example.messenger.presentation.components.common.LocalNavAnimatedVisibilityScope
import com.example.messenger.presentation.screens.ui.theme.BarPosition
import com.example.messenger.presentation.screens.ui.theme.LocalTabBarPosition
import com.example.messenger.presentation.screens.ui.theme.Motion
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import com.example.messenger.presentation.screens.ui.theme.MessengerShapes
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.presentation.components.call.CallAwareTopBar
import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.model.UserPresence
import com.example.messenger.presentation.components.common.PresenceIndicator
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme
import com.example.messenger.presentation.screens.ui.theme.PrimaryBlue
import com.example.messenger.presentation.state.ConversationsUiState
import com.example.messenger.presentation.viewmodel.ConversationsViewModel
import com.example.messenger.util.DateUtils
import com.example.messenger.util.resolveDisplayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    viewModel: ConversationsViewModel = hiltViewModel(),
    onChatClick: (String, String, String, String?) -> Unit = { _, _, _, _ -> },
    onEditChats: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val barAtBottom = LocalTabBarPosition.current == BarPosition.BOTTOM

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Spacer(
            modifier = Modifier.height(
                (if (barAtBottom) 0.dp else 72.dp) + LocalCallBarInset.current,
            ),
        )
        Box(modifier = Modifier.weight(1f)) {
            ChatsTabContent(
                uiState = uiState,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    onLogoutClick: () -> Unit = {},
    onCallBack: (partnerId: String, partnerName: String, video: Boolean) -> Unit = { _, _, _ -> },
    viewModel: com.example.messenger.presentation.viewmodel.CallsViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel(),
) {
    val calls by viewModel.calls.collectAsStateWithLifecycle()
    val barAtBottom = LocalTabBarPosition.current == BarPosition.BOTTOM
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Spacer(
            modifier = Modifier.height(
                (if (barAtBottom) 0.dp else 64.dp) + LocalCallBarInset.current,
            ),
        )
        CallsScreenContent(
            calls = calls,
            onCallBack = onCallBack,
            modifier = Modifier.weight(1f),
        )
        if (barAtBottom) {
            Spacer(modifier = Modifier.navigationBarsPadding().height(64.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onLogoutClick: () -> Unit = {},
    onContactClick: (String) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        ContactsScreenContent(
            modifier = Modifier.weight(1f),
            onContactClick = { user -> onContactClick(user.id) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatsTabContent(
    uiState: ConversationsUiState,
    onRefresh: () -> Unit,
    onChatClick: (String, String, String, String?) -> Unit,
    onEditChats: () -> Unit,
    onMarkRead: (String) -> Unit,
    onMarkUnread: (String) -> Unit,
    onDeleteForMe: (String) -> Unit,
    onDeleteForEveryone: (String) -> Unit,
) {
    when {
        uiState.isLoading -> ChatsLoading()
        uiState.error != null -> ChatsError(message = uiState.error.asString(), onRetry = onRefresh)
        uiState.conversations.isEmpty() -> ChatsEmpty()
        else -> ChatsList(
            uiState = uiState,
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

@Composable
private fun ChatsLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PrimaryBlue)
    }
}

@Composable
private fun ChatsError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, color = Color.Red)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
            Text("Retry")
        }
    }
}

@Composable
private fun ChatsEmpty() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No conversations yet", color = Color.Gray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatsList(
    uiState: ConversationsUiState,
    onRefresh: () -> Unit,
    onChatClick: (String, String, String, String?) -> Unit,
    onEditChats: () -> Unit,
    onMarkRead: (String) -> Unit,
    onMarkUnread: (String) -> Unit,
    onDeleteForMe: (String) -> Unit,
    onDeleteForEveryone: (String) -> Unit,
) {
    val currentUserId = uiState.currentUserId
    val navScope = LocalNavAnimatedVisibilityScope.current
    val barAtBottom = LocalTabBarPosition.current == BarPosition.BOTTOM
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (barAtBottom) 116.dp else 96.dp),
        ) {
            itemsIndexed(uiState.conversations, key = { _, it -> it.id }) { index, conversation ->
                val otherIdx = conversation.participantIds.indexOfFirst { it != currentUserId }
                val otherUserId = conversation.participantIds.getOrNull(otherIdx) ?: ""
                val otherUserName = resolveDisplayName(
                    rawName = conversation.participantNames.getOrNull(otherIdx),
                    alias = uiState.aliases[otherUserId],
                )
                val collapseModifier = if (navScope != null) {
                    val stagger = (index * 20).coerceAtMost(160)
                    with(navScope) {
                        Modifier.animateEnterExit(
                            enter = expandVertically(
                                animationSpec = tween(Motion.durationMedium, delayMillis = stagger, easing = Motion.emphasized),
                            ) + fadeIn(
                                animationSpec = tween(Motion.durationMedium, delayMillis = stagger),
                            ),
                            exit = shrinkVertically(
                                animationSpec = tween(Motion.durationShort, easing = Motion.standard),
                            ) + fadeOut(
                                animationSpec = tween(Motion.durationShort),
                            ),
                        )
                    }
                } else {
                    Modifier
                }
                val avatarUrl = uiState.avatars[otherUserId]
                    ?: conversation.participantAvatars.getOrNull(otherIdx)
                SwipeableConversationRow(
                    displayName = otherUserName,
                    isOneOnOne = conversation.participantIds.size == 2,
                    hasUnread = conversation.unreadCount > 0,
                    onClick = { onChatClick(conversation.id, otherUserId, otherUserName, avatarUrl) },
                    onEdit = onEditChats,
                    onMarkRead = { onMarkRead(conversation.id) },
                    onMarkUnread = { onMarkUnread(conversation.id) },
                    onDeleteForMe = { onDeleteForMe(conversation.id) },
                    onDeleteForEveryone = { onDeleteForEveryone(conversation.id) },
                    modifier = collapseModifier.animateItem(),
                ) { rowOnClick ->
                    ConversationListItem(
                        conversation = conversation,
                        partnerId = otherUserId,
                        displayName = otherUserName,
                        presence = uiState.presenceMap[otherUserId],
                        currentUserId = currentUserId,
                        onClick = rowOnClick,
                        photoUrl = avatarUrl,
                    )
                }
            }
        }
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
        )
    }
}
