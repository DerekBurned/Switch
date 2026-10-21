package com.example.messenger.presentation.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.model.presence.PresenceState
import com.example.messenger.presentation.components.common.MessengerAvatar
import com.example.messenger.presentation.components.common.PresenceIndicator
import com.example.messenger.presentation.components.common.sharedElementKey
import com.example.messenger.presentation.featureflag.featureEnabled
import com.example.messenger.presentation.theme.MessengerTheme
import com.example.messenger.presentation.theme.messengerTokens

@Composable
fun ChatTopBar(
    partnerName: String,
    presenceStatusText: String,
    statusAccent: Boolean,
    presenceState: PresenceState,
    isTyping: Boolean,
    sharedKeyPartnerId: String,
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCallClick: () -> Unit = {},
    onVideoCallClick: () -> Unit = {},
    partnerAvatarUrl: String? = null,
    partnerDeleted: Boolean = false,
) {
    val tokens = messengerTokens
    val barShape = RoundedCornerShape(26.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 10.dp, end = 10.dp, top = 6.dp)
            .clip(barShape)
            .background(tokens.cardFill)
            .border(1.dp, tokens.panelBorder, barShape)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 46.dp)
                .clip(CircleShape)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = tokens.textPrimary,
            )
        }
        Box(contentAlignment = Alignment.BottomEnd) {
            MessengerAvatar(
                name = partnerName,
                photoUrl = partnerAvatarUrl,
                deleted = partnerDeleted,
                size = 40.dp,
                modifier = Modifier
                    .sharedElementKey("avatar-$sharedKeyPartnerId")
                    .clip(CircleShape)
                    .clickable(onClick = onProfileClick),
            )
            if (!partnerDeleted) {
                PresenceIndicator(
                    state = presenceState,
                    size = 12.dp,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onProfileClick)
                .padding(start = 4.dp),
        ) {
            Text(
                text = partnerName.ifBlank { "Chat" },
                color = tokens.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.sharedElementKey("name-$sharedKeyPartnerId"),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isTyping) "Typing" else presenceStatusText,
                    color = if (statusAccent || isTyping) tokens.callAccept else tokens.textMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isTyping) {
                    Spacer(modifier = Modifier.width(4.dp))
                    BouncingDots()
                }
            }
        }
        if (featureEnabled(FeatureFlag.CALLS)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !partnerDeleted, onClick = onCallClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Voice call",
                    tint = if (partnerDeleted) tokens.textMuted else tokens.textPrimary,
                )
            }
        }
        if (featureEnabled(FeatureFlag.CALLS_VIDEO)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !partnerDeleted, onClick = onVideoCallClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video call",
                    tint = if (partnerDeleted) tokens.textMuted else tokens.textPrimary,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ChatTopBarPreview() {
    MessengerTheme {
        ChatTopBar(
            partnerName = "Alice",
            presenceStatusText = "online",
            statusAccent = true,
            presenceState = PresenceState.ONLINE,
            sharedKeyPartnerId = "preview",
            isTyping = false,
            onBackClick = {},
            onProfileClick = {},
        )
    }
}
