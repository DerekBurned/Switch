package com.example.messenger.presentation.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.presentation.theme.messengerTokens

@Composable
fun ReplyQuote(
    senderLabel: String?,
    text: String?,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = messengerTokens
    val accent = tokens.accent
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (tokens.isDark) Color.White.copy(alpha = 0.12f)
                else Color.Black.copy(alpha = 0.05f),
            )
            .clickable(onClick = onClick)
            .height(IntrinsicSize.Min)
            .padding(end = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accent),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.padding(vertical = 3.dp)) {
            Text(
                text = senderLabel ?: "Reply",
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = text.orEmpty(),
                color = contentColor.copy(alpha = 0.8f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
