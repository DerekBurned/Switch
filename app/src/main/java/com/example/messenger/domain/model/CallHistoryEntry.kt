package com.example.messenger.domain.model

import androidx.compose.runtime.Immutable
import com.example.messenger.domain.model.call.CallType

@Immutable
data class CallHistoryEntry(
    val id: String,
    val conversationId: String,
    val partnerId: String,
    val partnerName: String,
    val partnerAvatarUrl: String?,
    val timestamp: Long,
    val callType: CallType,
    val video: Boolean,
    val durationSeconds: Int,
    val outgoing: Boolean,
) {
    val missed: Boolean get() = callType == CallType.MISSED && !outgoing
}
