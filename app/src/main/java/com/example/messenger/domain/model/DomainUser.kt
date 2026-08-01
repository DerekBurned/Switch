package com.example.messenger.domain.model

data class DomainUser(
    val id: String,
    val displayName: String? = null,
    val phoneNumber: String? = null,
    val photoUrl: String? = null,
)
