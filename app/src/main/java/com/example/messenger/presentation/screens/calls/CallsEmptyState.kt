package com.example.messenger.presentation.screens.calls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.presentation.theme.messengerTokens

@Composable
internal fun ColumnScope.CallsEmptyState(missed: Boolean) {
    val tokens = messengerTokens
    Box(
        modifier = Modifier.fillMaxSize().weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (missed) Icons.Filled.CallMissed else Icons.Filled.Call,
                contentDescription = null,
                tint = tokens.textPrimary.copy(alpha = 0.4f),
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (missed) "No missed calls" else "No calls yet",
                color = tokens.textPrimary.copy(alpha = 0.8f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
