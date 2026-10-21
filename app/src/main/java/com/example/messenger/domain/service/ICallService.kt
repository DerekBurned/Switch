package com.example.messenger.domain.service

import android.view.View

interface ICallService {
    fun joinChannel(channelName: String, uid: Int, token: String? = null)
    fun renewToken(token: String)
    fun leaveChannel()
    fun muteLocalAudio(mute: Boolean)
    fun setSpeakerphone(enable: Boolean)
    fun enableLocalVideo(enable: Boolean)
    fun switchCamera()
    fun bindLocalVideo(view: View)
    fun bindRemoteVideo(view: View, uid: Int)
    fun unbindLocalVideo()
    fun unbindRemoteVideo(uid: Int)
    fun setRemoteVideoQuality(uid: Int, low: Boolean)
    fun setEventListener(listener: CallEventListener)

    fun clearEventListener(listener: CallEventListener)
}
