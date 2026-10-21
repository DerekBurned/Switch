package com.example.messenger.data.repository

sealed interface RemoteConversations {

    data class Server(val ids: Set<String>) : RemoteConversations
    data object Unavailable : RemoteConversations
}
