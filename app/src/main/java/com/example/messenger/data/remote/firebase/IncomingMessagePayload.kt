package com.example.messenger.data.remote.firebase

import com.example.messenger.data.remote.dto.message.RemoteMessageDto
import com.example.messenger.domain.model.Message

fun clipLabelFor(msgType: String, voice: String, video: String): String? = when (msgType) {
    Message.TYPE_VOICE_CLIP -> voice
    Message.TYPE_VIDEO_CLIP -> video
    else -> null
}

fun parseIncomingDto(data: Map<String, String>): RemoteMessageDto? {
    val conversationId = data["conversationId"].orEmpty()
    val messageId = data["messageId"].orEmpty()
    if (conversationId.isBlank() || messageId.isBlank()) return null
    return RemoteMessageDto(
        id = messageId,
        conversationId = conversationId,
        senderId = data["senderId"].orEmpty(),
        timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis(),
        status = "SENT",
        type = data["msgType"] ?: "TEXT",
        text = data["preview"].orEmpty(),
        enc = data["enc"]?.toIntOrNull() ?: 0,
        ciphertext = data["ciphertext"].orEmpty(),
        nonce = data["nonce"].orEmpty(),
        senderEpoch = data["senderEpoch"]?.toIntOrNull() ?: 0,
        recipientEpoch = data["recipientEpoch"]?.toIntOrNull() ?: 0,
        recipientId = data["recipientId"].orEmpty(),
    )
}
