package com.example.messenger.presentation.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.messenger.presentation.theme.messengerTokens

@Composable
internal fun ProfileDisplayRow(label: String, value: String) {
    val tokens = messengerTokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            color = tokens.textMuted,
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value.ifBlank { "—" },
            color = tokens.textPrimary,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
