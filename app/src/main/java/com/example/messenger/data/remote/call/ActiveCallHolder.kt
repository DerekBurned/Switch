package com.example.messenger.data.remote.call

import com.example.messenger.domain.service.CallConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object ActiveCallHolder {

    data class ActiveCall(
        val callId: String,
        val callerId: String,
        val calleeId: String,
        val channelName: String,
        val partnerName: String,
        val partnerPhone: String,
        val isIncoming: Boolean,

        val wasIncoming: Boolean = isIncoming,
        val isActive: Boolean = false,
        val remotePresent: Boolean = false,
        val callEnded: Boolean = false,

        val remoteRinging: Boolean = false,
        val muted: Boolean = false,
        val speakerOn: Boolean = false,
        val isVideoCall: Boolean = false,
        val localVideoOn: Boolean = false,
        val remoteVideoOn: Boolean = false,
        val frontCamera: Boolean = true,
        val seconds: Int = 0,
        val connectionState: CallConnectionState = CallConnectionState.CONNECTING,
        val error: String? = null,
    )

    private val _state = MutableStateFlow<ActiveCall?>(null)
    val state: StateFlow<ActiveCall?> = _state.asStateFlow()

    internal fun set(call: ActiveCall) {
        _state.value = call
    }

    internal fun update(transform: (ActiveCall) -> ActiveCall) {
        _state.update { it?.let(transform) }
    }

    internal fun clear() {
        _state.value = null
    }

    fun snapshot(): ActiveCall? = _state.value
}
