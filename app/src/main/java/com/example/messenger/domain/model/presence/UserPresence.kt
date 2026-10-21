package com.example.messenger.domain.model.presence

data class UserPresence(
    val state: PresenceState = PresenceState.OFFLINE,
    val lastSeen: Long = 0L
)
