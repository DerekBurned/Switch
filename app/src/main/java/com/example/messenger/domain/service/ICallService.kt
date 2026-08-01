package com.example.messenger.domain.service

import android.view.SurfaceView

interface ICallService {
    fun joinChannel(channelName: String, uid: Int, token: String? = null)
    fun renewToken(token: String)
    fun leaveChannel()
    fun muteLocalAudio(mute: Boolean)
    fun setSpeakerphone(enable: Boolean)
    fun enableLocalVideo(enable: Boolean)
    fun switchCamera()
    fun bindLocalVideo(view: SurfaceView)
    fun bindRemoteVideo(view: SurfaceView, uid: Int)
    fun setEventListener(listener: CallEventListener)

    fun clearEventListener(listener: CallEventListener)
}

enum class CallConnectionState { CONNECTING, CONNECTED, RECONNECTING, FAILED, DISCONNECTED }

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
