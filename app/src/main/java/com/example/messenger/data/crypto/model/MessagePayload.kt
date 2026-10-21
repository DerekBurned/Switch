package com.example.messenger.data.crypto.model

import kotlinx.serialization.Serializable

@Serializable
data class MessagePayload(
    val text: String = "",
    val replyToText: String? = null,
    val media: List<MediaSecret> = emptyList(),
    val clip: ClipSecret? = null,
)
