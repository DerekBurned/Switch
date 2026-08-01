package com.example.messenger.data.remote.call.telecom

import java.util.concurrent.ConcurrentHashMap

object ActiveConnectionHolder {

    private const val MAX_PENDING = 16

    private val connections = ConcurrentHashMap<String, MessengerConnection>()
    private val pendingDisconnects = ConcurrentHashMap<String, Int>()

    fun put(callId: String, connection: MessengerConnection) {
        if (callId.isBlank()) return
        val pendingCause = pendingDisconnects.remove(callId)
        if (pendingCause != null) {
            connection.markDisconnected(pendingCause)
            return
        }
        connections[callId] = connection
    }

    fun get(callId: String): MessengerConnection? = connections[callId]

    fun remove(callId: String) {
        connections.remove(callId)
    }

    fun disconnect(callId: String, cause: Int) {
        if (callId.isBlank()) return
        val connection = connections.remove(callId)
        if (connection != null) {
            connection.markDisconnected(cause)
        } else {
            rememberPending(callId, cause)
        }
    }

    fun disconnectAll(cause: Int) {
        val snapshot = connections.values.toList()
        connections.clear()
        pendingDisconnects.clear()
        snapshot.forEach { it.markDisconnected(cause) }
    }

    private fun rememberPending(callId: String, cause: Int) {
        if (pendingDisconnects.size >= MAX_PENDING) {
            pendingDisconnects.keys.firstOrNull()?.let { pendingDisconnects.remove(it) }
        }
        pendingDisconnects[callId] = cause
    }
}
