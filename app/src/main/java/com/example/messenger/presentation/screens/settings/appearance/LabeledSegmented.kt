package com.example.messenger.presentation.screens.settings.appearance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.presentation.components.common.SegmentedToggle
import com.example.messenger.presentation.theme.messengerTokens

@Composable
internal fun LabeledSegmented(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val tokens = messengerTokens
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(text = label, color = tokens.textPrimary, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))
        SegmentedToggle(
            options = options,
            selectedIndex = selectedIndex.coerceAtLeast(0),
            onSelect = onSelect,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
