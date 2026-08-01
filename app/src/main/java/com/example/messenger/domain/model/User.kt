package com.example.messenger.domain.model

data class User(
    val id: String = "",
    val username: String? = null,
    val usernameLower: String? = null,
    val email: String? = null,
    val phoneNumber: PhoneNumber? = null,
    val avatarUrl: String? = null,
    val lastSeen: Long = 0L,
    val isOnline: Boolean = false,
    val phoneVisibility: PhoneVisibility = PhoneVisibility.HIDDEN,
    val fcmToken: String? = null
)