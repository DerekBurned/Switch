package com.example.messenger.presentation.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.featureflag.FeatureFlags
import com.example.messenger.domain.usecase.conversation.MarkConversationAsReadUseCase
import com.example.messenger.domain.usecase.message.SendMessageUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationActionEntryPoint {
    fun sendMessageUseCase(): SendMessageUseCase
    fun markConversationAsReadUseCase(): MarkConversationAsReadUseCase
    fun featureFlags(): FeatureFlags
}

class NotificationReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID).orEmpty()
        if (conversationId.isBlank()) return

        val appContext = context.applicationContext
        val entry = EntryPointAccessors.fromApplication(appContext, NotificationActionEntryPoint::class.java)
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val featureFlags = entry.featureFlags()
                featureFlags.awaitFresh()
                when (intent.action) {
                    ACTION_OPEN_BUBBLE -> ChatNotifier.openAsBubble(
                        appContext,
                        conversationId,
                        intent.getStringExtra(EXTRA_PARTNER_ID).orEmpty(),
                        intent.getStringExtra(EXTRA_PARTNER_NAME).orEmpty(),
                    )
                    ACTION_REPLY ->
                        if (featureFlags.isEnabled(FeatureFlag.NOTIFICATION_INLINE_REPLY)) {
                            handleReply(appContext, intent, conversationId, entry)
                        } else {
                            ChatNotifier.refresh(appContext, conversationId)
                        }
                    ACTION_MARK_READ ->
                        if (featureFlags.isEnabled(FeatureFlag.NOTIFICATION_MARK_READ)) {
                            handleMarkRead(appContext, conversationId, entry)
                        } else {
                            ChatNotifier.refresh(appContext, conversationId)
                        }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "notification action failed", t)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handleReply(
        context: Context,
        intent: Intent,
        conversationId: String,
        entry: NotificationActionEntryPoint,
    ) {
        val text = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(KEY_TEXT_REPLY)
            ?.toString()
            .orEmpty()
            .trim()
        if (text.isBlank()) return
        entry.sendMessageUseCase().invoke(conversationId, text).collect { }
        ChatNotifier.appendOutgoing(context, conversationId, text, System.currentTimeMillis())
    }

    private suspend fun handleMarkRead(
        context: Context,
        conversationId: String,
        entry: NotificationActionEntryPoint,
    ) {
        entry.markConversationAsReadUseCase().invoke(conversationId)
        ChatNotifier.clear(context, conversationId)
    }

    companion object {
        private const val TAG = "NotifReplyReceiver"
        const val ACTION_REPLY = "com.example.messenger.notification.REPLY"
        const val ACTION_MARK_READ = "com.example.messenger.notification.MARK_READ"
        const val ACTION_OPEN_BUBBLE = "com.example.messenger.notification.OPEN_BUBBLE"
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
        const val EXTRA_PARTNER_ID = "extra_partner_id"
        const val EXTRA_PARTNER_NAME = "extra_partner_name"
    }
}
