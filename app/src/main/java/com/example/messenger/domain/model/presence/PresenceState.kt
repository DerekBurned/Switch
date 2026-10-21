package com.example.messenger.domain.model.presence

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
