package com.example.messenger.presentation.screens.settings.language

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.messenger.presentation.theme.Dimens
import com.example.messenger.presentation.theme.messengerTokens

@Composable
internal fun LanguageRow(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = messengerTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.rowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            color = tokens.textPrimary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = tokens.accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
