package com.example.messenger.data.remote.call.telecom

import android.os.Bundle

data class TelecomCallMeta(
    val callId: String,
    val callerId: String,
    val calleeId: String,
    val channelName: String,
    val partnerName: String,
    val partnerPhone: String,
    val isIncoming: Boolean,
    val isVideo: Boolean = false,
) {
    fun toBundle(): Bundle = Bundle().apply {
        putString(KEY_CALL_ID, callId)
        putString(KEY_CALLER_ID, callerId)
        putString(KEY_CALLEE_ID, calleeId)
        putString(KEY_CHANNEL_NAME, channelName)
        putString(KEY_PARTNER_NAME, partnerName)
        putString(KEY_PARTNER_PHONE, partnerPhone)
        putBoolean(KEY_IS_INCOMING, isIncoming)
        putBoolean(KEY_IS_VIDEO, isVideo)
    }

    companion object {
        private const val KEY_CALL_ID = "telecom_call_id"
        private const val KEY_CALLER_ID = "telecom_caller_id"
        private const val KEY_CALLEE_ID = "telecom_callee_id"
        private const val KEY_CHANNEL_NAME = "telecom_channel_name"
        private const val KEY_PARTNER_NAME = "telecom_partner_name"
        private const val KEY_PARTNER_PHONE = "telecom_partner_phone"
        private const val KEY_IS_INCOMING = "telecom_is_incoming"
        private const val KEY_IS_VIDEO = "telecom_is_video"

        fun fromBundle(bundle: Bundle?, isIncoming: Boolean): TelecomCallMeta? {
            bundle ?: return null
            val callId = bundle.getString(KEY_CALL_ID).orEmpty()
            if (callId.isBlank()) return null
            return TelecomCallMeta(
                callId = callId,
                callerId = bundle.getString(KEY_CALLER_ID).orEmpty(),
                calleeId = bundle.getString(KEY_CALLEE_ID).orEmpty(),
                channelName = bundle.getString(KEY_CHANNEL_NAME).orEmpty(),
                partnerName = bundle.getString(KEY_PARTNER_NAME).orEmpty(),
                partnerPhone = bundle.getString(KEY_PARTNER_PHONE).orEmpty(),
                isIncoming = bundle.getBoolean(KEY_IS_INCOMING, isIncoming),
                isVideo = bundle.getBoolean(KEY_IS_VIDEO, false),
            )
        }
    }
}
