package com.example.messenger.presentation.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.presentation.components.common.MotionSprings
import com.example.messenger.presentation.components.common.pressScale
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import androidx.compose.ui.tooling.preview.Preview
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme

@Composable
fun ScrollToBottomButton(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = messengerTokens
    val interaction = remember { MutableInteractionSource() }
    Box(modifier = modifier.pressScale(interaction, pressedScale = 0.9f)) {
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    onClick = onClick,
                ),
            shape = CircleShape,
            color = tokens.cardFill,
            shadowElevation = 6.dp,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Scroll to latest messages",
                    tint = tokens.accent,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        AnimatedVisibility(
            visible = unreadCount > 0,
            enter = scaleIn(animationSpec = MotionSprings.pop) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Surface(
                shape = CircleShape,
                color = tokens.accent,
                modifier = Modifier
                    .offset(x = 4.dp, y = (-6).dp)
                    .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                        color = tokens.onAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ScrollToBottomButtonPreview() {
    MessengerTheme {
        ScrollToBottomButton(unreadCount = 5, onClick = {})
    }
}
