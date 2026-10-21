package com.example.messenger.presentation.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.messenger.presentation.theme.messengerTokens

@Composable
internal fun AvatarSourceRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    val tokens = messengerTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(modifier = Modifier.size(16.dp))
        Text(text = label, color = tokens.textPrimary, style = MaterialTheme.typography.bodyLarge)
    }
}
