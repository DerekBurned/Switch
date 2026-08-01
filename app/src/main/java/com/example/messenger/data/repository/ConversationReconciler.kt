package com.example.messenger.data.repository

sealed interface RemoteConversations {
    
    data class Server(val ids: Set<String>) : RemoteConversations
    data object Unavailable : RemoteConversations
}

object ConversationReconciler {
    
    fun staleIds(local: Set<String>, remote: RemoteConversations): Set<String> =
        when (remote) {
            is RemoteConversations.Server -> local - remote.ids
            RemoteConversations.Unavailable -> emptySet()
        }
}
