package com.example.messenger.presentation.components.chat

import com.example.messenger.presentation.components.common.MessageStatusIcon
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.MissedVideoCall
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.presentation.screens.ui.theme.BubbleReceived
import com.example.messenger.presentation.screens.ui.theme.BubbleReceivedText
import com.example.messenger.presentation.screens.ui.theme.BubbleSent
import com.example.messenger.util.DateUtils
import androidx.compose.ui.tooling.preview.Preview
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme

@Composable
fun MissedCallCard(
    onCall: () -> Unit,
    timestamp: Long,
    isMe: Boolean,
    modifier: Modifier = Modifier,
    title: String = "Missed Call",
    status: MessageStatus? = null,
    video: Boolean = false,
) {

    val titleColor = if (isMe) Color.White else BubbleReceivedText
    val subtitleColor = if (isMe) Color.White.copy(alpha = 0.8f) else Color.Gray
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            onClick = onCall,
            shape = RoundedCornerShape(18.dp),
            color = if (isMe) BubbleSent else BubbleReceived,
            shadowElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (video) Icons.Default.MissedVideoCall else Icons.Default.CallMissed,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        color = titleColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tap to call back · ${DateUtils.formatMessageTime(timestamp)}",
                            color = subtitleColor,
                            fontSize = 12.sp,
                        )
                        if (isMe && status != null) {
                            Spacer(Modifier.width(4.dp))
                            MessageStatusIcon(status = status)
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF34C759), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (video) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = "Call back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun EndedCallCard(
    durationSeconds: Int,
    timestamp: Long,
    isMe: Boolean,
    modifier: Modifier = Modifier,
    video: Boolean = false,
) {
    val titleColor = if (isMe) Color.White else BubbleReceivedText
    val subtitleColor = if (isMe) Color.White.copy(alpha = 0.8f) else Color.Gray
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isMe) BubbleSent else BubbleReceived,
            shadowElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (video) Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = null,
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (video) "Video call" else "Call",
                        color = titleColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = "${DateUtils.formatDuration(durationSeconds)} · ${DateUtils.formatMessageTime(timestamp)}",
                        color = subtitleColor,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CallMessageCardsPreview() {
    MessengerTheme {
        Column {
            MissedCallCard(onCall = {}, timestamp = 1_700_000_000_000L, isMe = false)
            EndedCallCard(durationSeconds = 95, timestamp = 1_700_000_000_000L, isMe = true)
        }
    }
}
