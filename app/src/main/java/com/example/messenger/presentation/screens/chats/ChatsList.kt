package com.example.messenger.presentation.screens.chats

import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.presentation.components.common.LocalNavAnimatedVisibilityScope
import com.example.messenger.presentation.components.list.ConversationListItem
import com.example.messenger.presentation.components.list.SwipeableConversationRow
import com.example.messenger.presentation.featureflag.featureEnabled
import com.example.messenger.presentation.theme.LocalTabBarPosition
import com.example.messenger.presentation.theme.Motion
import com.example.messenger.presentation.theme.model.BarPosition
import com.example.messenger.util.resolveDisplayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatsList(
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
    val currentUserId = uiState.currentUserId
    val navScope = LocalNavAnimatedVisibilityScope.current
    val barAtBottom = LocalTabBarPosition.current == BarPosition.BOTTOM
    val refreshState = rememberPullToRefreshState()
    val pullEnabled = featureEnabled(FeatureFlag.CHAT_LIST_PULL_REFRESH)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                isRefreshing = uiState.isRefreshing,
                state = refreshState,
                enabled = pullEnabled,
                onRefresh = onRefresh,
            ),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = topInset,
                bottom = if (barAtBottom) 116.dp else 96.dp,
            ),
        ) {
            itemsIndexed(uiState.conversations, key = { _, it -> it.id }) { index, conversation ->
                val otherIdx = conversation.participantIds.indexOfFirst { it != currentUserId }
                val otherUserId = conversation.participantIds.getOrNull(otherIdx) ?: ""
                val participant = uiState.participants[otherUserId]
                val partnerDeleted = participant?.deleted == true
                val otherUserName = resolveDisplayName(
                    rawName = participant?.username
                        ?: conversation.participantNames.getOrNull(otherIdx),
                    alias = uiState.aliases[otherUserId],
                    deleted = partnerDeleted,
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
                val avatarUrl = if (partnerDeleted) null else uiState.avatars[otherUserId]
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
                        partnerDeleted = partnerDeleted,
                    )
                }
            }
        }
        if (pullEnabled) {
            PullToRefreshDefaults.Indicator(
                state = refreshState,
                isRefreshing = uiState.isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topInset),
            )
        }
    }
}
