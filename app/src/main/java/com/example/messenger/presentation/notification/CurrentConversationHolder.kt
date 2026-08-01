package com.example.messenger.presentation.notification

import java.util.concurrent.atomic.AtomicReference

object CurrentConversationHolder {
    private val ref = AtomicReference<String?>(null)

    fun setOpen(conversationId: String) {
        ref.set(conversationId)
    }

    fun clear(conversationId: String) {
        ref.compareAndSet(conversationId, null)
    }

    fun isOpen(conversationId: String): Boolean = ref.get() == conversationId
}
