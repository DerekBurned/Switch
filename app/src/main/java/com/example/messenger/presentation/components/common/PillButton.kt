package com.example.messenger.presentation.components.common
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.messenger.presentation.screens.ui.theme.Dimens
import com.example.messenger.presentation.screens.ui.theme.MessengerShapes
import com.example.messenger.presentation.screens.ui.theme.messengerTokens

enum class PillButtonStyle {
    Primary,
    Neutral,
    Danger,
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillButtonStyle = PillButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val tokens = messengerTokens
    val container = when (style) {
        PillButtonStyle.Primary -> tokens.accent
        PillButtonStyle.Neutral -> tokens.neutralButtonFill
        PillButtonStyle.Danger -> tokens.danger
    }
    val contentColor = when (style) {
        PillButtonStyle.Primary -> tokens.onAccent
        PillButtonStyle.Neutral -> tokens.textPrimary
        PillButtonStyle.Danger -> tokens.onAccent
    }
    val clickable = enabled && !loading
    val interaction = remember { MutableInteractionSource() }
    val containerColor by animateColorAsState(
        targetValue = container.copy(alpha = if (enabled) 1f else 0.5f),
        animationSpec = spring(stiffness = 700f),
        label = "pillButtonColor",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.buttonHeight)
            .pressScale(interaction, pressedScale = 0.97f)
            .clip(MessengerShapes.button)
            .background(containerColor)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = clickable,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = loading,
            modifier = Modifier.fillMaxSize(),
            animationSpec = spring(stiffness = 700f),
            label = "pillButtonContent",
        ) { isLoading ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = contentColor,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(26.dp),
                    )
                } else {
                    Text(
                        text = text,
                        color = contentColor,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PillButtonPreview() {
    MessengerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton(text = "Primary", onClick = {})
                PillButton(text = "Neutral", onClick = {}, style = PillButtonStyle.Neutral)
                PillButton(text = "Danger", onClick = {}, style = PillButtonStyle.Danger)
                PillButton(text = "Loading", onClick = {}, loading = true)
            }
        }
    }
}
