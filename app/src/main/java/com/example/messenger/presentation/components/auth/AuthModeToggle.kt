package com.example.messenger.presentation.components.auth
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AuthMode { LOGIN, REGISTER }

private val ToggleWidth = 260.dp
private val TogglePadding = 4.dp
private val HalfWidth = (ToggleWidth - TogglePadding * 2) / 2

@Composable
fun AuthModeToggle(
    mode: AuthMode,
    onModeChange: (AuthMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillOffset by animateDpAsState(
        targetValue = if (mode == AuthMode.LOGIN) 0.dp else HalfWidth,
        animationSpec = tween(durationMillis = 220),
        label = "auth-toggle-pill",
    )

    Box(
        modifier = modifier
            .width(ToggleWidth)
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(TogglePadding),
    ) {
        Box(
            modifier = Modifier
                .offset(x = pillOffset)
                .width(HalfWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White),
        )
        Row {
            ToggleHalf(
                label = "Log in",
                selected = mode == AuthMode.LOGIN,
                onClick = { onModeChange(AuthMode.LOGIN) },
            )
            ToggleHalf(
                label = "Register",
                selected = mode == AuthMode.REGISTER,
                onClick = { onModeChange(AuthMode.REGISTER) },
            )
        }
    }
}

@Composable
private fun ToggleHalf(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val textColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF16141B) else Color.White,
        animationSpec = tween(durationMillis = 220),
        label = "auth-toggle-text",
    )

    Box(
        modifier = Modifier
            .width(HalfWidth)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AuthModeTogglePreview() {
    MessengerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.padding(16.dp)) {
                AuthModeToggle(mode = AuthMode.LOGIN, onModeChange = {}, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
