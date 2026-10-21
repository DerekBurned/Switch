package com.example.messenger.domain.service

import com.example.messenger.domain.model.call.CallConnectionState

interface CallEventListener {
    fun onRemoteUserJoined(uid: Int)
    fun onRemoteUserLeft(uid: Int)
    fun onRemoteConnectionLost(uid: Int) {}
    fun onRemoteConnectionRestored(uid: Int) {}
    fun onRemoteVideoStateChanged(uid: Int, on: Boolean) {}
    fun onTokenPrivilegeWillExpire() {}
    fun onError(code: Int)

    fun onConnectionStateChanged(state: CallConnectionState) {}
}
