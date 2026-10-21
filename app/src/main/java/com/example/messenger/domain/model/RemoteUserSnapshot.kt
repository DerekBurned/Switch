package com.example.messenger.domain.model

data class RemoteUserSnapshot(
    val user: User?,
    val authoritative: Boolean,
)
