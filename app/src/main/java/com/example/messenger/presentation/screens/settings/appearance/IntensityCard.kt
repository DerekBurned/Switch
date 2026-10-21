package com.example.messenger.presentation.screens.settings.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.presentation.components.common.RoundedCard
import com.example.messenger.presentation.theme.messengerTokens
import kotlin.math.roundToInt

@Composable
internal fun IntensityCard(
    intensity: Float,
    onPreview: (Float) -> Unit,
    onCommit: (Float) -> Unit,
) {
    val tokens = messengerTokens
    var dragging by remember { mutableStateOf(false) }
    var localValue by remember { mutableStateOf(intensity) }
    if (!dragging) localValue = intensity
    RoundedCard {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Background intensity",
                    color = tokens.textPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${(localValue * 100).roundToInt()}%",
                    color = tokens.textMuted,
                    fontSize = 13.sp,
                )
            }
            Slider(
                value = localValue,
                onValueChange = {
                    dragging = true
                    localValue = it
                    onPreview(it)
                },
                onValueChangeFinished = {
                    onCommit(localValue)
                    dragging = false
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = tokens.accent,
                    activeTrackColor = tokens.accent,
                    inactiveTrackColor = tokens.pillFill,
                ),
            )
            Text(
                text = "Lower values soften the wallpaper toward a flat background",
                color = tokens.textMuted,
                fontSize = 12.sp,
            )
        }
    }
}
