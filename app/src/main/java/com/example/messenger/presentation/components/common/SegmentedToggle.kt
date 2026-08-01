package com.example.messenger.presentation.components.common
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.messenger.presentation.screens.ui.theme.MessengerShapes
import com.example.messenger.presentation.screens.ui.theme.TextOnFieldDark
import com.example.messenger.presentation.screens.ui.theme.messengerTokens

@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedContentColors: List<Color>? = null,
) {
    val tokens = messengerTokens
    val indicatorColor = tokens.pillFillSelected
    val indicatorPosition = remember { Animatable(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex) {
        indicatorPosition.animateTo(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
        )
    }
    Row(
        modifier = modifier
            .clip(MessengerShapes.tab)
            .background(tokens.trackFill)
            .border(1.dp, tokens.panelBorder, MessengerShapes.tab)
            .padding(4.dp)
            .drawBehind {
                if (options.isNotEmpty()) {
                    val segmentWidth = size.width / options.size
                    drawRoundRect(
                        color = indicatorColor,
                        topLeft = Offset(indicatorPosition.value * segmentWidth, 0f),
                        size = Size(segmentWidth, size.height),
                        cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                    )
                }
            },
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val interaction = remember { MutableInteractionSource() }
            val contentColor by animateColorAsState(
                targetValue = when {
                    selected -> selectedContentColors?.getOrNull(index) ?: TextOnFieldDark
                    else -> tokens.textPrimary.copy(alpha = 0.65f)
                },
                animationSpec = spring(stiffness = 700f),
                label = "segmentColor",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .pressScale(interaction, pressedScale = 0.96f)
                    .clip(MessengerShapes.tab)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                    ) { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SegmentedTogglePreview() {
    MessengerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SegmentedToggle(listOf("Register", "Login"), 1, {}, Modifier.fillMaxWidth())
                SegmentedToggle(listOf("All", "Missed"), 0, {}, Modifier.fillMaxWidth())
            }
        }
    }
}
