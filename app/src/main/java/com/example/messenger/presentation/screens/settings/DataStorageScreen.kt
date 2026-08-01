package com.example.messenger.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.messenger.presentation.components.common.CardDivider
import com.example.messenger.presentation.components.common.RoundedCard
import com.example.messenger.presentation.components.common.SectionHeader
import com.example.messenger.presentation.components.common.ToggleRow
import com.example.messenger.presentation.screens.ui.theme.MessengerShapes
import com.example.messenger.presentation.screens.ui.theme.messengerTokens

private data class StorageSegment(val label: String, val sizeLabel: String, val weight: Float, val color: Color)

@Composable
fun DataStorageScreen(onBack: () -> Unit) {
    var overWifi by rememberSaveable { mutableStateOf(true) }
    var overMobile by rememberSaveable { mutableStateOf(true) }
    val tokens = messengerTokens

    val segments = listOf(
        StorageSegment("Photo", "3 GB", 3f, tokens.accent),
        StorageSegment("Video", "1 GB", 1f, Color(0xFF4F8DF5)),
        StorageSegment("Files", "0.5 GB", 0.5f, Color(0xFFB07BE0)),
        StorageSegment("Cache", "0.5 GB", 0.5f, Color(0xFF9AA0A6)),
    )

    SettingsSubScaffold(title = "Data & Storage", onBack = onBack) {
        SectionHeader(text = "Storage")
        RoundedCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Storage",
                        color = tokens.textOnField,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "5 GB of 128 GB",
                        color = tokens.textMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                ) {
                    segments.forEach { segment ->
                        Box(
                            modifier = Modifier
                                .weight(segment.weight)
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(segment.color),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    segments.forEach { segment ->
                        StorageLegendItem(segment)
                    }
                }
            }
            CardDivider()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {}
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Clear cache",
                    color = tokens.accent,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        SectionHeader(text = "Media auto-download")
        RoundedCard {
            ToggleRow(title = "Over Wi-Fi", checked = overWifi, onCheckedChange = { overWifi = it })
            CardDivider()
            ToggleRow(
                title = "Over Mobile Data",
                checked = overMobile,
                onCheckedChange = { overMobile = it },
            )
        }
    }
}

@Composable
private fun StorageLegendItem(segment: StorageSegment) {
    val tokens = messengerTokens
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(segment.color),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "${segment.label} ${segment.sizeLabel}",
            color = tokens.textMuted,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
