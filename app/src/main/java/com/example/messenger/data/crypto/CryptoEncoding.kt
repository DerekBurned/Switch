package com.example.messenger.data.crypto

fun messageAad(conversationId: String, messageId: String, senderId: String, type: String): ByteArray =
    "$conversationId|$messageId|$senderId|$type".toByteArray()

fun b64(bytes: ByteArray): String = java.util.Base64.getEncoder().encodeToString(bytes)

fun unb64(value: String): ByteArray = java.util.Base64.getDecoder().decode(value)
