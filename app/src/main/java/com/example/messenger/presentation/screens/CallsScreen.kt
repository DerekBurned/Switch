package com.example.messenger.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.domain.model.CallHistoryEntry
import com.example.messenger.domain.model.CallType
import com.example.messenger.presentation.components.common.MessengerAvatar
import com.example.messenger.presentation.components.common.SegmentedToggle
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import com.example.messenger.util.DateUtils

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

@Composable
private fun CallHistoryRow(
    entry: CallHistoryEntry,
    onCallBack: (partnerId: String, partnerName: String, video: Boolean) -> Unit,
) {
    val tokens = messengerTokens
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCallBack(entry.partnerId, entry.partnerName, entry.video) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MessengerAvatar(name = entry.partnerName, photoUrl = entry.partnerAvatarUrl, size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.partnerName.ifBlank { "Unknown" },
                    color = if (entry.missed) tokens.danger else tokens.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                )
                Spacer(Modifier.height(1.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = directionIcon(entry),
                        contentDescription = null,
                        tint = if (entry.missed) tokens.danger else tokens.textMuted,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = subtitle(entry),
                        color = tokens.textMuted,
                        fontSize = 13.sp,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(tokens.pillFill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (entry.video) Icons.Filled.Videocam else Icons.Filled.Call,
                    contentDescription = if (entry.video) "Video call back" else "Call back",
                    tint = tokens.callAccept,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        HorizontalDivider(color = tokens.divider, thickness = 1.dp)
    }
}

private fun directionIcon(entry: CallHistoryEntry) = when {
    entry.missed -> Icons.Filled.CallMissed
    entry.outgoing -> Icons.AutoMirrored.Filled.CallMade
    else -> Icons.AutoMirrored.Filled.CallReceived
}

private fun subtitle(entry: CallHistoryEntry): String {
    val whenText = "${DateUtils.formatDayDivider(entry.timestamp)}, ${DateUtils.formatMessageTime(entry.timestamp)}"
    val label = when {
        entry.missed -> "Missed"
        entry.callType == CallType.UNREACHED -> "No answer"
        entry.callType == CallType.ENDED && entry.durationSeconds > 0 ->
            DateUtils.formatDuration(entry.durationSeconds)
        entry.outgoing -> "Outgoing"
        else -> "Incoming"
    }
    return "$label · $whenText"
}

@Composable
private fun ColumnScope.CallsEmptyState(missed: Boolean) {
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CallsScreenContentPreview() {
    MessengerTheme {
        CallsScreenContent(calls = emptyList())
    }
}
