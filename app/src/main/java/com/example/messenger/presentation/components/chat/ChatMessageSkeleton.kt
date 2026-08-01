package com.example.messenger.presentation.components.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import androidx.compose.ui.tooling.preview.Preview
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme

@Composable
fun ChatMessageSkeleton(modifier: Modifier = Modifier) {
    val shimmer = rememberInfiniteTransition(label = "chat-skeleton")
    val progress by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "chat-skeleton-progress",
    )
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        bubbles.forEach { (widthFraction, isMe) ->
            SkeletonBubble(widthFraction = widthFraction, isMe = isMe, progress = { progress })
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

private val bubbles = listOf(
    0.55f to false,
    0.72f to false,
    0.40f to true,
    0.62f to true,
    0.48f to false,
    0.66f to true,
)

@Composable
private fun SkeletonBubble(
    widthFraction: Float,
    isMe: Boolean,
    progress: () -> Float,
) {
    val tokens = messengerTokens
    val base = tokens.textPrimary.copy(alpha = 0.08f)
    val highlight = tokens.textPrimary.copy(alpha = 0.18f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height(44.dp)
                .clip(RoundedCornerShape(18.dp))
                .drawBehind {
                    val band = size.width * 0.6f
                    val start = -band + (size.width + band * 2f) * progress()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(base, highlight, base),
                            startX = start,
                            endX = start + band,
                        ),
                    )
                },
        )
    }
}

@Preview
@Composable
private fun ChatMessageSkeletonPreview() {
    MessengerTheme {
        ChatMessageSkeleton(modifier = Modifier.fillMaxWidth().height(360.dp))
    }
}
