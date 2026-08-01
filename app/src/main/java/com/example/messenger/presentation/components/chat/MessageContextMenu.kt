package com.example.messenger.presentation.components.chat
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.presentation.screens.ui.theme.messengerTokens

data class MessageAction(
    val title: String,
    val icon: ImageVector,
    val color: Color = Color.Black,
    val onClick: () -> Unit,
)

@Composable
fun ContextMenuContent(actions: List<MessageAction>) {
    Surface(
        modifier = Modifier
            .width(260.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = messengerTokens.cardFill,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
        ) {
            actions.forEachIndexed { index, action ->
                MenuItemRow(
                    title = action.title,
                    icon = action.icon,
                    color = action.color,
                    onClick = action.onClick,
                )

                if (index < actions.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = messengerTokens.divider,
                    )
                }
            }
        }
    }
}

@Composable
fun MenuItemRow(
    title: String,
    icon: ImageVector,
    color: Color = messengerTokens.textOnField,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            color = color,
            fontSize = 16.sp,
        )
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContextMenuContentPreview() {
    MessengerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.padding(16.dp)) {
                ContextMenuContent(
                    actions = listOf(
                        MessageAction("Copy", Icons.Default.ContentCopy, onClick = {}),
                        MessageAction("Edit", Icons.Default.Edit, onClick = {}),
                        MessageAction("Delete", Icons.Default.Delete, color = Color.Red, onClick = {}),
                    ),
                )
            }
        }
    }
}
