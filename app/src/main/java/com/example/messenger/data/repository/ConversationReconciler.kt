package com.example.messenger.data.repository

object ConversationReconciler {

    fun staleIds(local: Set<String>, remote: RemoteConversations): Set<String> =
        when (remote) {
            is RemoteConversations.Server -> local - remote.ids
            RemoteConversations.Unavailable -> emptySet()
        }
}
