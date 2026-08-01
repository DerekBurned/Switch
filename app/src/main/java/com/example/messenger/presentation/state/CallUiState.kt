package com.example.messenger.presentation.state

import com.example.messenger.domain.service.CallConnectionState
import com.example.messenger.presentation.base.UiState
import com.example.messenger.presentation.base.UiText

data class CallUiState(
    val partnerName: String = "",
    val partnerPhone: String = "",
    val partnerAvatarUrl: String? = null,
    val channelName: String = "",
    val isIncoming: Boolean = false,
    val isActive: Boolean = false,
    val remotePresent: Boolean = false,
    val callEnded: Boolean = false,

    val remoteRinging: Boolean = false,
    val seconds: Int = 0,
    val speakerOn: Boolean = false,
    val muted: Boolean = false,
    val isVideoCall: Boolean = false,
    val localVideoOn: Boolean = false,
    val remoteVideoOn: Boolean = false,
    val frontCamera: Boolean = true,
    val connectionState: CallConnectionState = CallConnectionState.CONNECTING,
    val error: UiText? = null,
) : UiState
