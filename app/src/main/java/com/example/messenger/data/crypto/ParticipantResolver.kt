package com.example.messenger.data.crypto

fun interface ParticipantResolver {
    suspend fun partnerOf(conversationId: String, myUid: String): String?
}
