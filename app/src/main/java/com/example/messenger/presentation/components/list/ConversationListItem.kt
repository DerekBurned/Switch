package com.example.messenger.presentation.components.list

import com.example.messenger.presentation.components.common.MessengerAvatar
import com.example.messenger.presentation.components.common.PresenceIndicator
import com.example.messenger.presentation.components.common.sharedElementKey
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.tooling.preview.Preview
import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.model.UserPresence
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import com.example.messenger.util.DateUtils

@Composable
fun ConversationListItem(
    conversation: Conversation,
    partnerId: String,
    displayName: String,
    presence: UserPresence?,
    currentUserId: String,
    onClick: () -> Unit,
    photoUrl: String? = null,
) {
    val tokens = messengerTokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(tokens.cardFill)
            .border(1.dp, tokens.panelBorder, RoundedCornerShape(26.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .height(74.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConversationAvatar(partnerId = partnerId, displayName = displayName, presence = presence, photoUrl = photoUrl)
            Spacer(modifier = Modifier.width(14.dp))
            ConversationPreview(
                displayName = displayName,
                partnerId = partnerId,
                lastMessage = conversation.lastMessage?.let { message ->
                    if (currentUserId.isNotBlank() && conversation.lastMessageSenderId == currentUserId) {
                        "You: $message"
                    } else {
                        message
                    }
                },
                hasUnread = conversation.unreadCount > 0,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(2.dp))
            ConversationMeta(
                timestamp = conversation.lastMessageTimestamp,
                unreadCount = conversation.unreadCount,
            )
        }
    }
}

@Composable
private fun ConversationAvatar(partnerId: String, displayName: String, presence: UserPresence?, photoUrl: String?) {
    Box(contentAlignment = Alignment.BottomEnd) {
        MessengerAvatar(
            name = displayName,
            photoUrl = photoUrl,
            size = 52.dp,
            modifier = Modifier.sharedElementKey("avatar-$partnerId"),
        )
        if (presence != null) {
            PresenceIndicator(state = presence.state, size = 14.dp)
        }
    }
}

@Composable
private fun ConversationPreview(
    displayName: String,
    partnerId: String,
    lastMessage: String?,
    hasUnread: Boolean,
    modifier: Modifier = Modifier,
) {
    val tokens = messengerTokens
    Column(modifier = modifier) {
        Text(
            text = displayName,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = tokens.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.sharedElementKey("name-$partnerId"),
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = lastMessage ?: "No messages yet",
            color = if (hasUnread) tokens.textPrimary.copy(alpha = 0.85f) else tokens.textMuted,
            fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ConversationMeta(timestamp: Long, unreadCount: Int) {
    val tokens = messengerTokens
    val hasUnread = unreadCount > 0
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.height(40.dp),
    ) {
        Text(
            text = DateUtils.formatMessageTime(timestamp),
            color = if (hasUnread) tokens.accent else tokens.textMuted,
            fontSize = 12.sp,
            fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
        )
        if (hasUnread) {
            UnreadCountBadge(count = unreadCount)
        } else {
            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}

@Composable
private fun UnreadCountBadge(count: Int) {
    val tokens = messengerTokens
    val label = if (count > 99) "99+" else count.toString()
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .clip(CircleShape)
            .background(tokens.accent)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = tokens.onAccent,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ConversationListItemPreview() {
    MessengerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ConversationListItem(
                conversation = Conversation(
                    id = "c1",
                    participantIds = listOf("u1", "u2"),
                    participantNames = listOf("Alice"),
                    lastMessage = "Hey, how are you?",
                    lastMessageSenderId = "u1",
                    lastMessageTimestamp = 1_700_000_000_000L,
                    unreadCount = 2,
                ),
                partnerId = "u2",
                displayName = "Alice",
                presence = null,
                currentUserId = "u1",
                onClick = {},
            )
        }
    }
}
