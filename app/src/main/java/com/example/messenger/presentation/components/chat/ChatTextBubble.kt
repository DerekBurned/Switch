package com.example.messenger.presentation.components.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import com.example.messenger.presentation.components.common.MessageStatusIcon
import com.example.messenger.presentation.screens.ui.theme.BubbleSent
import com.example.messenger.presentation.screens.ui.theme.BubbleSentTop
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import com.example.messenger.util.DateUtils
import androidx.compose.ui.tooling.preview.Preview
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme

@Composable
fun ChatTextBubble(
    message: ChatMessage,
    highlighted: Boolean = false,
    onReplyClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val highlightColor by animateColorAsState(
        targetValue = if (highlighted) Color(0x33FFC107) else Color.Transparent,
        label = "messageHighlight",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(highlightColor)
            .padding(vertical = 2.dp),
        horizontalArrangement = if (message.isMe) Arrangement.End else Arrangement.Start,
    ) {
        val tokens = messengerTokens
        Column(horizontalAlignment = if (message.isMe) Alignment.End else Alignment.Start) {
            val bubbleShape = if (message.isMe) {
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 5.dp, bottomStart = 18.dp)
            } else {
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 5.dp)
            }
            val onBubble = if (message.isMe) Color.White else tokens.textPrimary
            val metaColor = if (message.isMe) Color.White.copy(alpha = 0.85f) else tokens.textMuted
            val bubbleModifier = if (message.isMe) {
                Modifier
                    .widthIn(max = 300.dp)
                    .clip(bubbleShape)
                    .background(
                        brush = Brush.linearGradient(listOf(BubbleSentTop, BubbleSent)),
                        shape = bubbleShape,
                    )
            } else {
                Modifier
                    .widthIn(max = 300.dp)
                    .clip(bubbleShape)
                    .background(color = tokens.cardFill, shape = bubbleShape)
                    .border(width = 1.dp, color = tokens.panelBorder, shape = bubbleShape)
            }
            Box(
                modifier = bubbleModifier.padding(horizontal = 13.dp, vertical = 9.dp),
            ) {
                Column {
                    if (message.replyToMessageId != null) {
                        val accent = tokens.accent
                        Row(
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (tokens.isDark) Color.White.copy(alpha = 0.12f)
                                    else Color.Black.copy(alpha = 0.05f),
                                )
                                .clickable(onClick = onReplyClick)
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
                                    text = message.replyToSenderLabel ?: "Reply",
                                    color = accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = message.replyToText.orEmpty(),
                                    color = onBubble.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Text(
                        text = message.text,
                        color = onBubble,
                        fontSize = 15.sp,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 3.dp),
                    ) {
                        Text(
                            text = DateUtils.formatMessageTime(message.timestamp),
                            color = metaColor,
                            fontSize = 11.sp,
                        )
                        if (message.isMe) {
                            MessageStatusIcon(status = message.status)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ChatTextBubblePreview() {
    MessengerTheme {
        Column {
            ChatTextBubble(
                message = ChatMessage(text = "Hey, how are you?", isMe = false, timestamp = 1_700_000_000_000L),
            )
            ChatTextBubble(
                message = ChatMessage(
                    text = "All good, thanks!",
                    isMe = true,
                    timestamp = 1_700_000_060_000L,
                    replyToMessageId = "1",
                    replyToText = "Hey, how are you?",
                    replyToSenderLabel = "Alice",
                ),
            )
        }
    }
}
