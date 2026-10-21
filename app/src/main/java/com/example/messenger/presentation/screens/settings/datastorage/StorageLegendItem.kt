package com.example.messenger.presentation.screens.settings.datastorage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.messenger.presentation.theme.messengerTokens

@Composable
internal fun StorageLegendItem(segment: StorageSegment) {
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
