package com.example.messenger.domain.model

data class Profile(
    val id: String,
    val username: String?,
    val email: String? = null,
    val userId: String,
    val avatarUrl: String? = null,
    val lastSeen: Long = 0L,
    val isOnline: Boolean = false,
    val phoneVisibility: PhoneVisibility = PhoneVisibility.HIDDEN,
    val fcmToken: String? = null) {
}