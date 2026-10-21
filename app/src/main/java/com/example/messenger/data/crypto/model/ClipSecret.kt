package com.example.messenger.data.crypto.model

import kotlinx.serialization.Serializable

@Serializable
data class ClipSecret(
    val key: String? = null,
    val waveform: List<Int> = emptyList(),
)
