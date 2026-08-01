package com.example.messenger.presentation.components.list
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.domain.model.Conversation
import com.example.messenger.presentation.components.common.pressScale
import com.example.messenger.presentation.screens.ui.theme.LightGray
import com.example.messenger.presentation.screens.ui.theme.PrimaryBlue

@Composable
fun ForwardConversationRow(
    conversation: Conversation,
    currentUserId: String,
    onClick: () -> Unit,
) {
    val partnerIndex = conversation.participantIds.indexOfFirst { it != currentUserId }
        .takeIf { it >= 0 } ?: 0
    val name = conversation.participantNames.getOrNull(partnerIndex).orEmpty()
        .ifBlank { "Unknown" }
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction, pressedScale = 0.98f)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(46.dp).clip(CircleShape).background(LightGray),
            contentAlignment = Alignment.Center,
        ) {
            val initial = name.take(1).uppercase()
            if (initial.isNotBlank()) {
                Text(initial, color = PrimaryBlue, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(name, color = Color.Black, fontSize = 14.sp)
            conversation.lastMessage?.let {
                Text(it, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ForwardConversationRowPreview() {
    MessengerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ForwardConversationRow(
                conversation = Conversation(
                    id = "c1",
                    participantIds = listOf("me", "u2"),
                    participantNames = listOf("", "Alice"),
                    lastMessage = "See you soon",
                ),
                currentUserId = "me",
                onClick = {},
            )
        }
    }
}
