package com.example.messenger.presentation.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.content.LocusIdCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.IconCompat
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.example.messenger.R
import com.example.messenger.presentation.BubbleActivity
import com.example.messenger.presentation.MainActivity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

object ChatNotifier {

    const val GROUP_KEY = "chat_messages_group"
    private const val SUMMARY_ID = 0x53554D
    private const val MAX_MESSAGES = 8
    private const val BUBBLE_HEIGHT_DP = 600
    private const val AVATAR_SIZE_PX = 256
    private const val AVATAR_LOAD_TIMEOUT_MS = 5_000L

    private data class Entry(
        val text: String,
        val timestamp: Long,
        val senderId: String,
        val senderName: String,
        val fromMe: Boolean,
    )

    private class ConvState(
        val partnerId: String,
        val partnerName: String,
        var partnerAvatar: String,
        var partnerIcon: IconCompat? = null,
        val messages: MutableList<Entry> = mutableListOf(),
    )

    private val states = HashMap<String, ConvState>()
    private val lock = Any()

    fun notifyIncoming(
        context: Context,
        conversationId: String,
        senderId: String,
        senderName: String,
        text: String,
        timestamp: Long,
        senderAvatar: String = "",
    ) {
        val cachedIcon = synchronized(lock) {
            states[conversationId]?.takeIf { it.partnerAvatar == senderAvatar }?.partnerIcon
        }
        val icon = cachedIcon
            ?: loadAvatarIcon(context, senderAvatar)
            ?: monogramIcon(senderName)
        synchronized(lock) {
            val state = states.getOrPut(conversationId) {
                ConvState(partnerId = senderId, partnerName = senderName, partnerAvatar = senderAvatar)
            }
            if (senderAvatar.isNotBlank()) state.partnerAvatar = senderAvatar
            state.partnerIcon = icon
            state.messages.add(Entry(text, timestamp, senderId, senderName, fromMe = false))
            trim(state)
            ConversationShortcuts.push(context, conversationId, state.partnerId, state.partnerName, icon)
            post(context, conversationId, state, silent = false)
            postSummary(context)
        }
    }

    fun appendOutgoing(context: Context, conversationId: String, text: String, timestamp: Long) {
        synchronized(lock) {
            val state = states[conversationId] ?: return
            state.messages.add(Entry(text, timestamp, senderId = "", senderName = "", fromMe = true))
            trim(state)
            post(context, conversationId, state, silent = true)
        }
    }

    fun openAsBubble(context: Context, conversationId: String, partnerId: String, partnerName: String) {
        synchronized(lock) {
            val state = states[conversationId] ?: ConvState(
                partnerId = partnerId,
                partnerName = partnerName,
                partnerAvatar = "",
                partnerIcon = monogramIcon(partnerName),
            ).also { states[conversationId] = it }
            ConversationShortcuts.push(context, conversationId, state.partnerId, state.partnerName, state.partnerIcon)
            post(context, conversationId, state, silent = true, expandAsBubble = true)
        }
    }

    fun clear(context: Context, conversationId: String) {
        synchronized(lock) {
            states.remove(conversationId)
            val nm = context.getSystemService<NotificationManager>() ?: return
            nm.cancel(conversationId.hashCode())
            postSummary(context)
        }
    }

    fun notifyFallback(context: Context, title: String?, body: String?) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        val notification = NotificationCompat.Builder(context, NotificationChannels.CHAT_MESSAGES)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body.orEmpty()))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun trim(state: ConvState) {
        while (state.messages.size > MAX_MESSAGES) state.messages.removeAt(0)
    }

    private fun post(context: Context, conversationId: String, state: ConvState, silent: Boolean, expandAsBubble: Boolean = false) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        val me = Person.Builder().setName(context.getString(R.string.notif_message_you)).build()
        val style = NotificationCompat.MessagingStyle(me)
        state.messages.forEach { entry ->
            val person = if (entry.fromMe) {
                null
            } else {
                Person.Builder()
                    .setKey(entry.senderId)
                    .setName(entry.senderName)
                    .setIcon(state.partnerIcon)
                    .build()
            }
            style.addMessage(entry.text, entry.timestamp, person)
        }
        val latest = state.messages.lastOrNull()?.timestamp ?: System.currentTimeMillis()
        val builder = NotificationCompat.Builder(context, NotificationChannels.CHAT_MESSAGES)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSilent(silent)
            .setWhen(latest)
            .setShowWhen(true)
            .setGroup(GROUP_KEY)
            .setShortcutId(conversationId)
            .setLocusId(LocusIdCompat(conversationId))
            .setContentIntent(contentIntent(context, conversationId, state))
            .addAction(replyAction(context, conversationId, state))
            .addAction(openInWindowAction(context, conversationId, state))
            .addAction(markReadAction(context, conversationId))
        if (expandAsBubble) {
            builder.setBubbleMetadata(bubbleMetadata(context, conversationId, state))
        }
        nm.notify(conversationId.hashCode(), builder.build())
    }

    private fun bubbleMetadata(
        context: Context,
        conversationId: String,
        state: ConvState,
    ): NotificationCompat.BubbleMetadata {
        val intent = Intent(context, BubbleActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_CONVERSATION_ID, conversationId)
            putExtra(MainActivity.EXTRA_PARTNER_ID, state.partnerId)
            putExtra(MainActivity.EXTRA_PARTNER_NAME, state.partnerName)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (conversationId + ":bubble").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val icon = state.partnerIcon ?: IconCompat.createWithResource(context, R.drawable.ic_stat_notification)
        return NotificationCompat.BubbleMetadata.Builder(pendingIntent, icon)
            .setDesiredHeight(BUBBLE_HEIGHT_DP)
            .setAutoExpandBubble(true)
            .setSuppressNotification(true)
            .build()
    }

    private fun loadAvatarIcon(context: Context, url: String): IconCompat? {
        if (url.isBlank()) return null
        return runCatching {
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(AVATAR_SIZE_PX, AVATAR_SIZE_PX)
                .allowHardware(false)
                .build()
            val result = runBlocking {
                withTimeoutOrNull(AVATAR_LOAD_TIMEOUT_MS) {
                    SingletonImageLoader.get(context).execute(request)
                }
            }
            val bitmap = (result as? SuccessResult)?.image?.let { it as? BitmapImage }?.bitmap
            bitmap?.let { IconCompat.createWithAdaptiveBitmap(it) }
        }.getOrNull()
    }

    private fun monogramIcon(name: String): IconCompat {
        val size = AVATAR_SIZE_PX
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(colorForName(name))
        val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * 0.45f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val baseline = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(initial, size / 2f, baseline, textPaint)
        return IconCompat.createWithAdaptiveBitmap(bitmap)
    }

    private fun colorForName(name: String): Int {
        val palette = intArrayOf(
            0xFF5B8DEE.toInt(),
            0xFF4CAF50.toInt(),
            0xFFE57373.toInt(),
            0xFFBA68C8.toInt(),
            0xFFFFB300.toInt(),
            0xFF26A69A.toInt(),
        )
        val index = (name.hashCode() and 0x7FFFFFFF) % palette.size
        return palette[index]
    }

    private fun replyAction(
        context: Context,
        conversationId: String,
        state: ConvState,
    ): NotificationCompat.Action {
        val intent = Intent(context, NotificationReplyReceiver::class.java).apply {
            action = NotificationReplyReceiver.ACTION_REPLY
            putExtra(NotificationReplyReceiver.EXTRA_CONVERSATION_ID, conversationId)
            putExtra(NotificationReplyReceiver.EXTRA_PARTNER_ID, state.partnerId)
            putExtra(NotificationReplyReceiver.EXTRA_PARTNER_NAME, state.partnerName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (conversationId + ":reply").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val remoteInput = RemoteInput.Builder(NotificationReplyReceiver.KEY_TEXT_REPLY)
            .setLabel(context.getString(R.string.notif_action_reply_hint))
            .build()
        return NotificationCompat.Action.Builder(
            R.drawable.ic_stat_notification,
            context.getString(R.string.notif_action_reply),
            pendingIntent,
        )
            .addRemoteInput(remoteInput)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setAllowGeneratedReplies(true)
            .build()
    }

    private fun markReadAction(context: Context, conversationId: String): NotificationCompat.Action {
        val intent = Intent(context, NotificationReplyReceiver::class.java).apply {
            action = NotificationReplyReceiver.ACTION_MARK_READ
            putExtra(NotificationReplyReceiver.EXTRA_CONVERSATION_ID, conversationId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (conversationId + ":read").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(
            R.drawable.ic_stat_notification,
            context.getString(R.string.notif_action_mark_read),
            pendingIntent,
        )
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .build()
    }

    private fun openInWindowAction(
        context: Context,
        conversationId: String,
        state: ConvState,
    ): NotificationCompat.Action {
        val intent = Intent(context, NotificationReplyReceiver::class.java).apply {
            action = NotificationReplyReceiver.ACTION_OPEN_BUBBLE
            putExtra(NotificationReplyReceiver.EXTRA_CONVERSATION_ID, conversationId)
            putExtra(NotificationReplyReceiver.EXTRA_PARTNER_ID, state.partnerId)
            putExtra(NotificationReplyReceiver.EXTRA_PARTNER_NAME, state.partnerName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (conversationId + ":openwin").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(
            R.drawable.ic_stat_notification,
            context.getString(R.string.notif_action_open_window),
            pendingIntent,
        ).build()
    }

    private fun postSummary(context: Context) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        if (states.size < 2) {
            nm.cancel(SUMMARY_ID)
            return
        }
        val totalMessages = states.values.sumOf { it.messages.size }
        val inbox = NotificationCompat.InboxStyle()
            .setSummaryText(context.getString(R.string.notif_summary_new_messages, totalMessages))
        states.values.forEach { inbox.addLine(it.partnerName) }
        val summary = NotificationCompat.Builder(context, NotificationChannels.CHAT_MESSAGES)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setStyle(inbox)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .build()
        nm.notify(SUMMARY_ID, summary)
    }

    private fun contentIntent(context: Context, conversationId: String, state: ConvState): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_OPEN_CONVERSATION_ID, conversationId)
            putExtra(MainActivity.EXTRA_PARTNER_ID, state.partnerId)
            putExtra(MainActivity.EXTRA_PARTNER_NAME, state.partnerName)
        }
        return PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
