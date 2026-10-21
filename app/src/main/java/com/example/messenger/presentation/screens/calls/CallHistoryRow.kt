package com.example.messenger.presentation.screens.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.model.CallHistoryEntry
import com.example.messenger.domain.model.call.CallType
import com.example.messenger.presentation.components.common.MessengerAvatar
import com.example.messenger.presentation.featureflag.featureEnabled
import com.example.messenger.presentation.theme.messengerTokens
import com.example.messenger.util.DateUtils

@Composable
internal fun CallHistoryRow(
    entry: CallHistoryEntry,
    onCallBack: (partnerId: String, partnerName: String, video: Boolean) -> Unit,
) {
    val tokens = messengerTokens
    val callBackEnabled = featureEnabled(if (entry.video) FeatureFlag.CALLS_VIDEO else FeatureFlag.CALLS)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = callBackEnabled) { onCallBack(entry.partnerId, entry.partnerName, entry.video) }
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
