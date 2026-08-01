package com.example.messenger.presentation.notification

import android.content.Context
import android.content.Intent
import androidx.core.app.Person
import androidx.core.content.LocusIdCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.messenger.R
import com.example.messenger.presentation.MainActivity

object ConversationShortcuts {

    fun push(
        context: Context,
        conversationId: String,
        partnerId: String,
        partnerName: String,
        icon: IconCompat? = null,
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_OPEN_CONVERSATION_ID, conversationId)
            putExtra(MainActivity.EXTRA_PARTNER_ID, partnerId)
            putExtra(MainActivity.EXTRA_PARTNER_NAME, partnerName)
        }
        val resolvedIcon = icon ?: IconCompat.createWithResource(context, R.drawable.ic_stat_notification)
        val label = partnerName.ifBlank { context.getString(R.string.notif_message_fallback_sender) }
        val person = Person.Builder()
            .setKey(partnerId)
            .setName(label)
            .setIcon(resolvedIcon)
            .build()
        val shortcut = ShortcutInfoCompat.Builder(context, conversationId)
            .setShortLabel(label)
            .setLongLived(true)
            .setIntent(intent)
            .setPerson(person)
            .setIcon(resolvedIcon)
            .setLocusId(LocusIdCompat(conversationId))
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }
}
