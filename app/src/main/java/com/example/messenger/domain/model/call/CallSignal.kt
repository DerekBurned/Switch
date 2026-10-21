package com.example.messenger.domain.model.call

data class CallSignal(
    val callId: String = "",
    val callerId: String = "",
    val calleeId: String = "",
    val channelName: String = "",
    val status: CallStatus = CallStatus.RINGING,

    val timestamp: Long = 0L,
    val video: Boolean = false,
)
