package com.example.messenger.data.remote.dto

import com.example.messenger.domain.model.CallSignal
import com.example.messenger.domain.model.CallStatus

data class CallSignalDto(
    val callId: String = "",
    val callerId: String = "",
    val calleeId: String = "",
    val channelName: String = "",
    val status: String = "RINGING",
    val timestamp: Long = 0L,
    val video: Boolean = false,
)

fun CallSignalDto.toCallSignal() = CallSignal(
    callId = callId,
    callerId = callerId,
    calleeId = calleeId,
    channelName = channelName,
    status = runCatching { CallStatus.valueOf(status) }.getOrDefault(CallStatus.RINGING),
    timestamp = timestamp,
    video = video,
)

fun CallSignal.toDto() = CallSignalDto(
    callId = callId,
    callerId = callerId,
    calleeId = calleeId,
    channelName = channelName,
    status = status.name,
    timestamp = timestamp,
    video = video,
)
