package com.example.messenger.data.repository

import com.example.messenger.data.local.obx.ObxConversation
import com.example.messenger.data.local.obx.ObxConversation_
import com.example.messenger.data.local.obx.ObxMessage
import com.example.messenger.data.local.obx.ObxMessage_
import com.example.messenger.data.local.obx.asFlow
import com.example.messenger.data.local.obx.queryFlow
import com.example.messenger.domain.model.CallHistoryEntry
import com.example.messenger.domain.model.CallType
import com.example.messenger.domain.model.Message
import io.objectbox.Box
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallHistoryRepository @Inject constructor(
    private val messageBox: Box<ObxMessage>,
    private val conversationBox: Box<ObxConversation>,
) {

    fun getCallHistory(currentUserId: String): Flow<List<CallHistoryEntry>> {
        return queryFlow {
            messageBox.query(
                ObxMessage_.type.oneOf(CALL_TYPES, QueryBuilder.StringOrder.CASE_SENSITIVE),
            )
                .orderDesc(ObxMessage_.timestamp)
                .build()
        }.map { rows ->
            val liveRows = rows.filterNot { it.deleted }
            val conversationIds = liveRows.map { it.conversationId }.distinct().toTypedArray()
            val conversationsById = if (conversationIds.isEmpty()) {
                emptyMap()
            } else {
                conversationBox.query(ObxConversation_.uid.oneOf(conversationIds))
                    .build()
                    .use { it.find() }
                    .associateBy { it.uid }
            }
            liveRows.mapNotNull { it.toEntry(currentUserId, conversationsById[it.conversationId]) }
        }
    }

    private fun ObxMessage.toEntry(currentUserId: String, conversation: ObxConversation?): CallHistoryEntry? {
        conversation ?: return null
        val partnerIndex = conversation.participantIds.indexOfFirst { it != currentUserId }
        val partnerId = conversation.participantIds.getOrNull(partnerIndex)
            ?: conversation.participantIds.firstOrNull { it != currentUserId }
            ?: return null
        val partnerName = conversation.participantNames.getOrNull(partnerIndex).orEmpty()
        val partnerAvatar = conversation.participantAvatars.getOrNull(partnerIndex)
        return CallHistoryEntry(
            id = uid,
            conversationId = conversationId,
            partnerId = partnerId,
            partnerName = partnerName,
            partnerAvatarUrl = partnerAvatar,
            timestamp = timestamp,
            callType = type.toCallType(),
            video = callVideo,
            durationSeconds = callDurationSeconds,
            outgoing = senderId == currentUserId,
        )
    }

    private fun String.toCallType(): CallType = when (this) {
        Message.TYPE_MISSED_CALL -> CallType.MISSED
        Message.TYPE_UNREACHED_CALL -> CallType.UNREACHED
        else -> CallType.ENDED
    }

    private companion object {
        val CALL_TYPES = arrayOf(
            Message.TYPE_MISSED_CALL,
            Message.TYPE_UNREACHED_CALL,
            Message.TYPE_ENDED_CALL,
        )
    }
}
