package com.example.messenger.data.remote.dto

import com.example.messenger.domain.model.PhoneNumber

data class UserDTO(
    val id: String = "",
    val username: String? = null,
    val email: String? = null,
    val phoneNumber: PhoneNumber? = null,
    val avatarUrl: String? = null,
    val lastSeen: Long = 0L,
    val isOnline: Boolean = false,
    val fcmToken: String? = null,
    val phoneVisibility: String = "HIDDEN" 
)