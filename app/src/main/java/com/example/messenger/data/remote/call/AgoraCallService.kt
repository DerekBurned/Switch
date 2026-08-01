package com.example.messenger.data.remote.call

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.SurfaceView
import com.example.messenger.BuildConfig
import com.example.messenger.domain.service.CallConnectionState
import com.example.messenger.domain.service.CallEventListener
import com.example.messenger.domain.service.ICallService
import dagger.hilt.android.qualifiers.ApplicationContext
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgoraCallService @Inject constructor(
    @ApplicationContext private val ctx: Context
) : ICallService {
    private var engine: RtcEngine? = null
    private var listener: CallEventListener? = null
    private var videoModuleEnabled = false
    private var localVideoOn = false

    private val handler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.e(TAG, "onJoinChannelSuccess: local user joined channel=$channel as uid=$uid")
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.e(TAG, "onUserJoined: remote uid=$uid is now in the channel (peer present) -> onRemoteUserJoined")
            listener?.onRemoteUserJoined(uid)
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            val quit = reason == io.agora.rtc2.Constants.USER_OFFLINE_QUIT
            Log.e(
                TAG,
                "onUserOffline: remote uid=$uid reason=$reason (${userOfflineReasonName(reason)}) -> " +
                    if (quit) "end call (intentional hangup)" else "reconnecting (network drop)",
            )
            if (quit) {
                listener?.onRemoteUserLeft(uid)
            } else {
                listener?.onRemoteConnectionLost(uid)
            }
        }

        override fun onError(err: Int) {
            Log.e(TAG, "onError code=$err (${RtcEngine.getErrorDescription(err)})")
            listener?.onError(err)
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            Log.e(
                TAG,
                "onConnectionStateChanged: local state=$state (${connectionStateName(state)}) " +
                    "reason=$reason (${connectionChangedReasonName(reason)})",
            )
            listener?.onConnectionStateChanged(state.toCallConnectionState())
        }

        override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            Log.e(
                TAG,
                "onRemoteAudioStateChanged: remote uid=$uid state=$state (${remoteAudioStateName(state)}) " +
                    "reason=$reason (${remoteAudioReasonName(reason)})",
            )
            when (state) {
                io.agora.rtc2.Constants.REMOTE_AUDIO_STATE_STARTING,
                io.agora.rtc2.Constants.REMOTE_AUDIO_STATE_DECODING ->
                    listener?.onRemoteConnectionRestored(uid)
                io.agora.rtc2.Constants.REMOTE_AUDIO_STATE_FROZEN,
                io.agora.rtc2.Constants.REMOTE_AUDIO_STATE_FAILED ->
                    listener?.onRemoteConnectionLost(uid)
                io.agora.rtc2.Constants.REMOTE_AUDIO_STATE_STOPPED ->
                    if (reason != io.agora.rtc2.Constants.REMOTE_AUDIO_REASON_REMOTE_MUTED) {
                        listener?.onRemoteConnectionLost(uid)
                    }
            }
        }

        override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            Log.e(TAG, "onRemoteVideoStateChanged: remote uid=$uid state=$state reason=$reason")
            when (state) {
                io.agora.rtc2.Constants.REMOTE_VIDEO_STATE_STARTING,
                io.agora.rtc2.Constants.REMOTE_VIDEO_STATE_DECODING ->
                    listener?.onRemoteVideoStateChanged(uid, true)
                io.agora.rtc2.Constants.REMOTE_VIDEO_STATE_STOPPED,
                io.agora.rtc2.Constants.REMOTE_VIDEO_STATE_FAILED ->
                    listener?.onRemoteVideoStateChanged(uid, false)
            }
        }

        override fun onTokenPrivilegeWillExpire(token: String?) {
            Log.w(TAG, "onTokenPrivilegeWillExpire")
            listener?.onTokenPrivilegeWillExpire()
        }
    }

    init {
        require(BuildConfig.AGORA_APP_ID.isNotBlank()) {
            "BuildConfig.AGORA_APP_ID is missing. Set AGORA_APP_ID in gradle.properties / local.properties."
        }

        try {
            val config = RtcEngineConfig().apply {
                mContext = ctx
                mAppId = BuildConfig.AGORA_APP_ID
                mEventHandler = handler
            }
            val created = RtcEngine.create(config)
            if (created == null) {
                Log.e(TAG, "RtcEngine.create returned null — process ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
            } else {
                created.enableAudio()
            }
            engine = created
        } catch (t: Throwable) {

            Log.e(TAG, "Failed to create Agora RtcEngine", t)
        }
    }

    override fun joinChannel(channelName: String, uid: Int, token: String?) {
        val engine = engine
        if (engine == null) {

            Log.e(TAG, "joinChannel called but engine is unavailable")
            listener?.onError(ERR_ENGINE_UNAVAILABLE)
            return
        }
        val options = ChannelMediaOptions().apply {
            channelProfile = io.agora.rtc2.Constants.CHANNEL_PROFILE_COMMUNICATION
            clientRoleType = io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER
            publishMicrophoneTrack = true
            publishCameraTrack = localVideoOn
            autoSubscribeVideo = true
        }
        Log.e(TAG, "joinChannel channel=$channelName uid=$uid video=$localVideoOn tokenized=${!token.isNullOrBlank()}")
        engine.joinChannel(token.orEmpty(), channelName, uid, options)
    }

    override fun renewToken(token: String) {
        engine?.renewToken(token)
    }

    override fun leaveChannel() {
        engine?.let { engine ->
            if (localVideoOn) {
                localVideoOn = false
                engine.stopPreview()
                engine.enableLocalVideo(false)
            }
            engine.leaveChannel()
        }
    }

    override fun muteLocalAudio(mute: Boolean) {
        engine?.muteLocalAudioStream(mute)
    }

    override fun setSpeakerphone(enable: Boolean) {
        engine?.setEnableSpeakerphone(enable)
    }

    override fun enableLocalVideo(enable: Boolean) {
        val engine = engine ?: return
        if (localVideoOn == enable) return
        localVideoOn = enable
        if (enable) {
            ensureVideoModule(engine)
            engine.enableLocalVideo(true)
            engine.startPreview()
        } else {
            engine.stopPreview()
            engine.enableLocalVideo(false)
        }
        engine.updateChannelMediaOptions(
            ChannelMediaOptions().apply { publishCameraTrack = enable },
        )
    }

    override fun switchCamera() {
        engine?.switchCamera()
    }

    override fun bindLocalVideo(view: SurfaceView) {
        val engine = engine ?: return
        ensureVideoModule(engine)
        engine.setupLocalVideo(VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, 0))
    }

    override fun bindRemoteVideo(view: SurfaceView, uid: Int) {
        val engine = engine ?: return
        ensureVideoModule(engine)
        engine.setupRemoteVideo(VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, uid))
    }

    private fun ensureVideoModule(engine: RtcEngine) {
        if (videoModuleEnabled) return
        videoModuleEnabled = true
        engine.enableVideo()
    }

    override fun setEventListener(listener: CallEventListener) {
        this.listener = listener
    }

    override fun clearEventListener(listener: CallEventListener) {
        if (this.listener === listener) this.listener = null
    }

    private fun Int.toCallConnectionState(): CallConnectionState = when (this) {
        io.agora.rtc2.Constants.CONNECTION_STATE_CONNECTING -> CallConnectionState.CONNECTING
        io.agora.rtc2.Constants.CONNECTION_STATE_CONNECTED -> CallConnectionState.CONNECTED
        io.agora.rtc2.Constants.CONNECTION_STATE_RECONNECTING -> CallConnectionState.RECONNECTING
        io.agora.rtc2.Constants.CONNECTION_STATE_FAILED -> CallConnectionState.FAILED
        io.agora.rtc2.Constants.CONNECTION_STATE_DISCONNECTED -> CallConnectionState.DISCONNECTED
        else -> CallConnectionState.DISCONNECTED
    }

    private fun connectionStateName(state: Int): String = when (state) {
        io.agora.rtc2.Constants.CONNECTION_STATE_DISCONNECTED -> "DISCONNECTED: not in the channel"
        io.agora.rtc2.Constants.CONNECTION_STATE_CONNECTING -> "CONNECTING: joining the channel"
        io.agora.rtc2.Constants.CONNECTION_STATE_CONNECTED -> "CONNECTED: media is flowing"
        io.agora.rtc2.Constants.CONNECTION_STATE_RECONNECTING -> "RECONNECTING: lost connection, retrying"
        io.agora.rtc2.Constants.CONNECTION_STATE_FAILED -> "FAILED: cannot connect"
        else -> "unknown state $state"
    }

    private fun connectionChangedReasonName(reason: Int): String = when (reason) {
        io.agora.rtc2.Constants.CONNECTION_CHANGED_CONNECTING -> "CONNECTING"
        io.agora.rtc2.Constants.CONNECTION_CHANGED_JOIN_SUCCESS -> "JOIN_SUCCESS"
        io.agora.rtc2.Constants.CONNECTION_CHANGED_INTERRUPTED -> "INTERRUPTED: network interrupted"
        io.agora.rtc2.Constants.CONNECTION_CHANGED_BANNED_BY_SERVER -> "BANNED_BY_SERVER"
        io.agora.rtc2.Constants.CONNECTION_CHANGED_JOIN_FAILED -> "JOIN_FAILED"
        io.agora.rtc2.Constants.CONNECTION_CHANGED_LEAVE_CHANNEL -> "LEAVE_CHANNEL"
        io.agora.rtc2.Constants.CONNECTION_CHANGED_INVALID_TOKEN -> "INVALID_TOKEN"
        io.agora.rtc2.Constants.CONNECTION_CHANGED_TOKEN_EXPIRED -> "TOKEN_EXPIRED"
        io.agora.rtc2.Constants.CONNECTION_CHANGED_REJECTED_BY_SERVER -> "REJECTED_BY_SERVER"
        io.agora.rtc2.Constants.CONNECTION_CHANGED_KEEP_ALIVE_TIMEOUT -> "KEEP_ALIVE_TIMEOUT: no heartbeat"
        io.agora.rtc2.Constants.CONNECTION_CHANGED_REJOIN_SUCCESS -> "REJOIN_SUCCESS: reconnected"
        io.agora.rtc2.Constants.CONNECTION_CHANGED_LOST -> "LOST: network gone"
        else -> "reason $reason"
    }

    private fun userOfflineReasonName(reason: Int): String = when (reason) {
        io.agora.rtc2.Constants.USER_OFFLINE_QUIT -> "QUIT: left the channel on purpose"
        io.agora.rtc2.Constants.USER_OFFLINE_DROPPED -> "DROPPED: timed out, likely lost network"
        io.agora.rtc2.Constants.USER_OFFLINE_BECOME_AUDIENCE -> "BECOME_AUDIENCE: stopped publishing"
        else -> "reason $reason"
    }

    private fun remoteAudioStateName(state: Int): String = when (state) {
        io.agora.rtc2.Constants.REMOTE_AUDIO_STATE_STOPPED -> "STOPPED: no remote audio"
        io.agora.rtc2.Constants.REMOTE_AUDIO_STATE_STARTING -> "STARTING: first packet received"
        io.agora.rtc2.Constants.REMOTE_AUDIO_STATE_DECODING -> "DECODING: audio playing normally"
        io.agora.rtc2.Constants.REMOTE_AUDIO_STATE_FROZEN -> "FROZEN: playback stalled (network)"
        io.agora.rtc2.Constants.REMOTE_AUDIO_STATE_FAILED -> "FAILED: remote audio failed"
        else -> "unknown state $state"
    }

    private fun remoteAudioReasonName(reason: Int): String = when (reason) {
        io.agora.rtc2.Constants.REMOTE_AUDIO_REASON_INTERNAL -> "INTERNAL"
        io.agora.rtc2.Constants.REMOTE_AUDIO_REASON_NETWORK_CONGESTION -> "NETWORK_CONGESTION: peer's network is bad"
        io.agora.rtc2.Constants.REMOTE_AUDIO_REASON_NETWORK_RECOVERY -> "NETWORK_RECOVERY: peer's network recovered"
        io.agora.rtc2.Constants.REMOTE_AUDIO_REASON_LOCAL_MUTED -> "LOCAL_MUTED"
        io.agora.rtc2.Constants.REMOTE_AUDIO_REASON_LOCAL_UNMUTED -> "LOCAL_UNMUTED"
        io.agora.rtc2.Constants.REMOTE_AUDIO_REASON_REMOTE_MUTED -> "REMOTE_MUTED: peer muted their mic"
        io.agora.rtc2.Constants.REMOTE_AUDIO_REASON_REMOTE_UNMUTED -> "REMOTE_UNMUTED: peer unmuted"
        io.agora.rtc2.Constants.REMOTE_AUDIO_REASON_REMOTE_OFFLINE -> "REMOTE_OFFLINE: peer left"
        io.agora.rtc2.Constants.REMOTE_AUDIO_REASON_NO_PACKET_RECEIVE -> "NO_PACKET_RECEIVE: no audio packets arriving"
        io.agora.rtc2.Constants.REMOTE_AUDIO_REASON_LOCAL_PLAY_FAILED -> "LOCAL_PLAY_FAILED"
        else -> "reason $reason"
    }

    private companion object {
        const val TAG = "AgoraCallService"
        
        const val ERR_ENGINE_UNAVAILABLE = -1
    }
}
