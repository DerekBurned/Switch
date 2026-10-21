package com.example.messenger.data.crypto.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaSecret(
    val itemId: String,
    val key: String,
    val blurHash: String = "",
)
