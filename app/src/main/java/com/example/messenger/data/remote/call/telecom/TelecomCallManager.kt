package com.example.messenger.data.remote.call.telecom

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelecomCallManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val telecomManager: TelecomManager? = context.getSystemService()
    private val phoneAccountHandle = PhoneAccountHandle(
        ComponentName(context, MessengerConnectionService::class.java),
        ACCOUNT_ID,
    )

    @Suppress("DEPRECATION")
    fun registerPhoneAccount() {
        val tm = telecomManager ?: return
        val account = PhoneAccount.builder(phoneAccountHandle, ACCOUNT_LABEL)
            .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
            .build()
        runCatching { tm.registerPhoneAccount(account) }
            .onFailure { Log.w(TAG, "registerPhoneAccount failed", it) }
    }

    @SuppressLint("MissingPermission")
    fun addIncoming(meta: TelecomCallMeta): Boolean {
        val tm = telecomManager ?: return false
        if (ActiveConnectionHolder.get(meta.callId) != null) return true
        return runCatching {
            val extras = Bundle().apply {
                putParcelable(
                    TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
                    Uri.fromParts("sip", meta.callerId, null),
                )
                putBundle(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS, meta.toBundle())
            }
            tm.addNewIncomingCall(phoneAccountHandle, extras)
            true
        }.getOrElse {
            Log.w(TAG, "addNewIncomingCall failed", it)
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun placeOutgoing(meta: TelecomCallMeta): Boolean {
        val tm = telecomManager ?: return false
        return runCatching {
            val uri = Uri.fromParts("sip", meta.calleeId, null)
            val extras = Bundle().apply {
                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
                putBundle(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, meta.toBundle())
            }
            tm.placeCall(uri, extras)
            true
        }.getOrElse {
            Log.w(TAG, "placeCall failed", it)
            false
        }
    }

    private companion object {
        const val TAG = "TelecomCallManager"
        const val ACCOUNT_ID = "messenger_self_managed"
        const val ACCOUNT_LABEL = "Messenger"
    }
}
