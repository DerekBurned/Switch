package com.example.messenger.presentation.screens.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.messenger.domain.model.CallHistoryEntry
import com.example.messenger.presentation.components.common.SegmentedToggle
import com.example.messenger.presentation.theme.MessengerTheme
import com.example.messenger.presentation.theme.messengerTokens

enum class CallsFilter { ALL, MISSED }

@Composable
fun CallsScreenContent(
    calls: List<CallHistoryEntry>,
    modifier: Modifier = Modifier,
    onCallBack: (partnerId: String, partnerName: String, video: Boolean) -> Unit = { _, _, _ -> },
) {
    var filter by remember { mutableStateOf(CallsFilter.ALL) }
    val tokens = messengerTokens

    val visible = remember(calls, filter) {
        if (filter == CallsFilter.MISSED) calls.filter { it.missed } else calls
    }

    Column(modifier = modifier.fillMaxSize()) {
        SegmentedToggle(
            options = listOf("All", "Missed"),
            selectedIndex = if (filter == CallsFilter.ALL) 0 else 1,
            onSelect = { filter = if (it == 0) CallsFilter.ALL else CallsFilter.MISSED },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )

        if (visible.isEmpty()) {
            CallsEmptyState(missed = filter == CallsFilter.MISSED)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(tokens.cardFill)
                    .border(1.dp, tokens.panelBorder, RoundedCornerShape(26.dp)),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(visible, key = { it.id }) { entry ->
                    CallHistoryRow(entry = entry, onCallBack = onCallBack)
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CallsScreenContentPreview() {
    MessengerTheme {
        CallsScreenContent(calls = emptyList())
    }
}
