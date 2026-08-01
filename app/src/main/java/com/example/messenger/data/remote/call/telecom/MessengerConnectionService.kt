package com.example.messenger.data.remote.call.telecom

import android.net.Uri
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log

class MessengerConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ): Connection? {
        val meta = TelecomCallMeta.fromBundle(
            request?.extras?.getBundle(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS),
            isIncoming = true,
        ) ?: return null

        val connection = MessengerConnection(applicationContext, meta).apply {
            setCallerDisplayName(meta.partnerName, TelecomManager.PRESENTATION_ALLOWED)
            setAddress(Uri.fromParts("sip", meta.callerId, null), TelecomManager.PRESENTATION_ALLOWED)
            setRinging()
        }
        ActiveConnectionHolder.put(meta.callId, connection)
        return connection
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ): Connection? {
        val meta = TelecomCallMeta.fromBundle(
            request?.extras?.getBundle(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS),
            isIncoming = false,
        ) ?: return null

        val connection = MessengerConnection(applicationContext, meta).apply {
            setCallerDisplayName(meta.partnerName, TelecomManager.PRESENTATION_ALLOWED)
            setAddress(Uri.fromParts("sip", meta.calleeId, null), TelecomManager.PRESENTATION_ALLOWED)
            setDialing()
        }
        ActiveConnectionHolder.put(meta.callId, connection)
        return connection
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ) {
        Log.w(TAG, "incoming connection refused by Telecom; call still handled by foreground service")
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ) {
        Log.w(TAG, "outgoing connection refused by Telecom; call still handled by foreground service")
    }

    private companion object {
        const val TAG = "MsgConnectionService"
    }
}
