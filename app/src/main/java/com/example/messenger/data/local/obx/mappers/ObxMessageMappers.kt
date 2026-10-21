package com.example.messenger.data.local.obx.mappers

import com.example.messenger.data.local.obx.ClipPayload
import com.example.messenger.data.local.obx.ObxMessage
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.call.CallType
import com.example.messenger.domain.model.voicemessage.Clip

fun ObxMessage.toDomain(): Message {
    val parsedStatus = parseStatus(status)

    fun asText(): Message = Message.Text(
        id = uid,
        conversationId = conversationId,
        senderId = senderId,
        timestamp = timestamp,
        status = parsedStatus,
        isRead = isRead,
        deleted = deleted,
        text = text,
        replyToMessageId = replyToMessageId,
        replyToText = replyToText,
        replyToSenderId = replyToSenderId,
    )

    fun asClip(clip: Clip): Message = Message.Clip(
        id = uid,
        conversationId = conversationId,
        senderId = senderId,
        timestamp = timestamp,
        status = parsedStatus,
        isRead = isRead,
        deleted = deleted,
        clip = clip,
        played = clipPlayed,
        replyToMessageId = replyToMessageId,
        replyToText = replyToText,
        replyToSenderId = replyToSenderId,
    )

    return when (type.ifBlank { Message.TYPE_TEXT }) {
        Message.TYPE_MEDIA -> Message.Media(
            id = uid,
            conversationId = conversationId,
            senderId = senderId,
            timestamp = timestamp,
            status = parsedStatus,
            isRead = isRead,
            deleted = deleted,
            items = mediaItemsJson,
            caption = text,
            replyToMessageId = replyToMessageId,
            replyToText = replyToText,
            replyToSenderId = replyToSenderId,
        )
        Message.TYPE_MISSED_CALL -> Message.Call(
            id = uid,
            conversationId = conversationId,
            senderId = senderId,
            timestamp = timestamp,
            status = parsedStatus,
            isRead = isRead,
            deleted = deleted,
            callType = CallType.MISSED,
            video = callVideo,
        )
        Message.TYPE_UNREACHED_CALL -> Message.Call(
            id = uid,
            conversationId = conversationId,
            senderId = senderId,
            timestamp = timestamp,
            status = parsedStatus,
            isRead = isRead,
            deleted = deleted,
            callType = CallType.UNREACHED,
            video = callVideo,
        )
        Message.TYPE_ENDED_CALL -> Message.Call(
            id = uid,
            conversationId = conversationId,
            senderId = senderId,
            timestamp = timestamp,
            status = parsedStatus,
            isRead = isRead,
            deleted = deleted,
            callType = CallType.ENDED,
            durationSeconds = callDurationSeconds,
            video = callVideo,
        )
        Message.TYPE_VOICE_CLIP -> clipJson?.let { asClip(it.toVoiceClip()) } ?: asText()
        Message.TYPE_VIDEO_CLIP -> clipJson?.let { asClip(it.toVideoClip()) } ?: asText()
        else -> asText()
    }
}

fun Message.toObx(): ObxMessage = when (this) {
    is Message.Text -> ObxMessage(
        uid = id,
        conversationId = conversationId,
        senderId = senderId,
        timestamp = timestamp,
        status = status.name,
        isRead = isRead,
        deleted = deleted,
        type = Message.TYPE_TEXT,
        text = text,
        replyToMessageId = replyToMessageId,
        replyToText = replyToText,
        replyToSenderId = replyToSenderId,
        callDurationSeconds = 0,
        mediaItemsJson = emptyList(),
    )
    is Message.Media -> ObxMessage(
        uid = id,
        conversationId = conversationId,
        senderId = senderId,
        timestamp = timestamp,
        status = status.name,
        isRead = isRead,
        deleted = deleted,
        type = Message.TYPE_MEDIA,
        text = caption,
        replyToMessageId = replyToMessageId,
        replyToText = replyToText,
        replyToSenderId = replyToSenderId,
        callDurationSeconds = 0,
        mediaItemsJson = items,
    )
    is Message.Call -> ObxMessage(
        uid = id,
        conversationId = conversationId,
        senderId = senderId,
        timestamp = timestamp,
        status = status.name,
        isRead = isRead,
        deleted = deleted,
        type = when (callType) {
            CallType.MISSED -> Message.TYPE_MISSED_CALL
            CallType.UNREACHED -> Message.TYPE_UNREACHED_CALL
            CallType.ENDED -> Message.TYPE_ENDED_CALL
        },
        text = "",
        callDurationSeconds = durationSeconds,
        callVideo = video,
        mediaItemsJson = emptyList(),
    )
    is Message.Clip -> ObxMessage(
        uid = id,
        conversationId = conversationId,
        senderId = senderId,
        timestamp = timestamp,
        status = status.name,
        isRead = isRead,
        deleted = deleted,
        type = when (clip) {
            is Clip.Voice -> Message.TYPE_VOICE_CLIP
            is Clip.Video -> Message.TYPE_VIDEO_CLIP
        },
        text = "",
        replyToMessageId = replyToMessageId,
        replyToText = replyToText,
        replyToSenderId = replyToSenderId,
        callDurationSeconds = 0,
        mediaItemsJson = emptyList(),
        clipJson = clip.toPayload(),
        clipPlayed = played,
    )
}

private fun Clip.toPayload(): ClipPayload = when (this) {
    is Clip.Voice -> ClipPayload(
        url = url,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        fileKey = fileKey,
        waveform = waveform,
    )
    is Clip.Video -> ClipPayload(
        url = url,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        fileKey = fileKey,
    )
}

private fun ClipPayload.toVoiceClip(): Clip = Clip.Voice(
    url = url,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    fileKey = fileKey,
    waveform = waveform,
)

private fun ClipPayload.toVideoClip(): Clip = Clip.Video(
    url = url,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    fileKey = fileKey,
)

private fun parseStatus(value: String): MessageStatus {
    if (value == "DELIVERED") return MessageStatus.SENT
    return runCatching { MessageStatus.valueOf(value) }.getOrDefault(MessageStatus.SENT)
}
