package com.example.messenger.data.remote.call.telecom

import android.content.Context
import android.content.Intent
import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.DisconnectCause
import com.example.messenger.data.remote.call.CallForegroundService

class MessengerConnection(
    private val appContext: Context,
    val meta: TelecomCallMeta,
) : Connection() {

    init {
        connectionProperties = PROPERTY_SELF_MANAGED
        audioModeIsVoip = true
        connectionCapabilities = CAPABILITY_MUTE
    }

    override fun onAnswer() {
        sendServiceAction(CallForegroundService.ACTION_ACCEPT)
    }

    override fun onReject() {
        sendServiceAction(CallForegroundService.ACTION_DECLINE)
        markDisconnected(DisconnectCause.REJECTED)
    }

    override fun onDisconnect() {
        sendServiceAction(CallForegroundService.ACTION_END)
        markDisconnected(DisconnectCause.LOCAL)
    }

    override fun onAbort() {
        sendServiceAction(CallForegroundService.ACTION_END)
        markDisconnected(DisconnectCause.CANCELED)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onCallAudioStateChanged(state: CallAudioState) {
        val speaker = state.route == CallAudioState.ROUTE_SPEAKER
        val intent = Intent(appContext, CallForegroundService::class.java)
            .setAction(CallForegroundService.ACTION_SET_SPEAKER)
            .putExtra(CallForegroundService.EXTRA_SPEAKER, speaker)
        runCatching { appContext.startService(intent) }
    }

    fun markActive() {
        runCatching { setActive() }
    }

    @Suppress("DEPRECATION")
    fun applyAudioRoute(speaker: Boolean) {
        runCatching {
            setAudioRoute(if (speaker) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
        }
    }

    fun markDisconnected(cause: Int) {
        runCatching {
            setDisconnected(DisconnectCause(cause))
            destroy()
        }
        ActiveConnectionHolder.remove(meta.callId)
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(appContext, CallForegroundService::class.java).setAction(action)
        runCatching { appContext.startService(intent) }
    }
}
