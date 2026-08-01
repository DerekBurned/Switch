package com.example.messenger.domain.model

enum class PresenceState {
    ONLINE,
    AWAY,
    OFFLINE;

    companion object {
        fun fromString(value: String?): PresenceState {
            return when (value?.lowercase()) {
                "online" -> ONLINE
                "away" -> AWAY
                else -> OFFLINE
            }
        }
    }
}

data class UserPresence(
    val state: PresenceState = PresenceState.OFFLINE,
    val lastSeen: Long = 0L
)
