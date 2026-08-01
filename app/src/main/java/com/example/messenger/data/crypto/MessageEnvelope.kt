package com.example.messenger.data.crypto

import kotlinx.serialization.Serializable

@Serializable
data class MessagePayload(
    val text: String = "",
    val replyToText: String? = null,
    val media: List<MediaSecret> = emptyList(),
)

@Serializable
data class MediaSecret(
    val itemId: String,
    val key: String,
    val blurHash: String = "",
)

fun messageAad(conversationId: String, messageId: String, senderId: String, type: String): ByteArray =
    "$conversationId|$messageId|$senderId|$type".toByteArray()

fun b64(bytes: ByteArray): String = java.util.Base64.getEncoder().encodeToString(bytes)

fun unb64(value: String): ByteArray = java.util.Base64.getDecoder().decode(value)
