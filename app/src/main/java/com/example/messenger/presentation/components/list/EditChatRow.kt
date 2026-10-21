package com.example.messenger.presentation.components.list
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.domain.model.Conversation
import com.example.messenger.presentation.components.common.MessengerAvatar
import com.example.messenger.presentation.components.common.pressScale
import com.example.messenger.presentation.theme.MessengerTheme
import com.example.messenger.presentation.theme.messengerTokens

@Composable
fun EditChatRow(conversation: Conversation, selected: Boolean, onClick: () -> Unit) {
    val tokens = messengerTokens
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction, pressedScale = 0.98f)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MessengerAvatar(
            name = conversation.participantNames.firstOrNull().orEmpty(),
            size = 46.dp,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = conversation.participantNames.firstOrNull() ?: "Unknown",
            color = tokens.textPrimary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (selected) tokens.accent else Color.Transparent)
                .border(2.dp, if (selected) tokens.accent else tokens.panelBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EditChatRowPreview() {
    MessengerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                EditChatRow(Conversation(id = "c1", participantNames = listOf("Alice")), selected = true, onClick = {})
                EditChatRow(Conversation(id = "c2", participantNames = listOf("Bob")), selected = false, onClick = {})
            }
        }
    }
}
