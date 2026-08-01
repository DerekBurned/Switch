package com.example.messenger.domain.model

data class CallSignal(
    val callId: String = "",
    val callerId: String = "",
    val calleeId: String = "",
    val channelName: String = "",
    val status: CallStatus = CallStatus.RINGING,

    val timestamp: Long = 0L,
    val video: Boolean = false,
)

enum class CallStatus { RINGING, ACTIVE, DECLINED, ENDED }
