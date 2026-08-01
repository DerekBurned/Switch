package com.example.messenger.presentation.notification

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.content.getSystemService
import com.example.messenger.R

object NotificationChannels {

    const val CHAT_MESSAGES = "chat_messages_channel"

    const val INCOMING_CALL = "call_channel"

    const val ONGOING_CALL = "ongoing_call_channel"

    const val MISSED_CALLS = "missed_calls_channel"

    private const val GROUP_MESSAGES = "messages_group"
    private const val GROUP_CALLS = "calls_group"

    fun ensureCreated(context: Context) {
        val nm = context.getSystemService<NotificationManager>() ?: return

        nm.createNotificationChannelGroup(
            NotificationChannelGroup(GROUP_MESSAGES, context.getString(R.string.notif_group_messages)),
        )
        nm.createNotificationChannelGroup(
            NotificationChannelGroup(GROUP_CALLS, context.getString(R.string.notif_group_calls)),
        )

        if (nm.getNotificationChannel(CHAT_MESSAGES) == null) {
            val chat = NotificationChannel(
                CHAT_MESSAGES,
                context.getString(R.string.notif_channel_messages_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notif_channel_messages_desc)
                group = GROUP_MESSAGES
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setAllowBubbles(true)
                }
            }
            nm.createNotificationChannel(chat)
        }

        if (nm.getNotificationChannel(INCOMING_CALL) == null) {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val call = NotificationChannel(
                INCOMING_CALL,
                context.getString(R.string.notif_channel_incoming_call_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notif_channel_incoming_call_desc)
                group = GROUP_CALLS
                setBypassDnd(true)
                enableLights(true)
                enableVibration(true)
                setSound(ringtoneUri, audioAttrs)
            }
            nm.createNotificationChannel(call)
        }

        if (nm.getNotificationChannel(ONGOING_CALL) == null) {
            val ongoing = NotificationChannel(
                ONGOING_CALL,
                context.getString(R.string.notif_channel_ongoing_call_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notif_channel_ongoing_call_desc)
                group = GROUP_CALLS
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
            }
            nm.createNotificationChannel(ongoing)
        }

        if (nm.getNotificationChannel(MISSED_CALLS) == null) {
            val missed = NotificationChannel(
                MISSED_CALLS,
                context.getString(R.string.notif_channel_missed_call_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notif_channel_missed_call_desc)
                group = GROUP_CALLS
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            nm.createNotificationChannel(missed)
        }
    }
}
