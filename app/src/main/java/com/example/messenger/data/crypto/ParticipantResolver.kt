package com.example.messenger.data.crypto

import com.example.messenger.data.local.obx.ObxConversation
import com.example.messenger.data.local.obx.ObxConversation_
import com.google.firebase.firestore.FirebaseFirestore
import io.objectbox.Box
import kotlinx.coroutines.tasks.await

fun interface ParticipantResolver {
    suspend fun partnerOf(conversationId: String, myUid: String): String?
}

class ObxParticipantResolver(
    private val conversationBox: Box<ObxConversation>,
    private val firestore: FirebaseFirestore,
) : ParticipantResolver {

    override suspend fun partnerOf(conversationId: String, myUid: String): String? {
        val local = conversationBox.query(ObxConversation_.uid.equal(conversationId)).build()
            .use { it.findFirst() }
        val participants = local?.participantIds ?: remoteParticipants(conversationId) ?: return null
        if (participants.size != 2) return null
        return participants.firstOrNull { it != myUid }
    }

    private suspend fun remoteParticipants(conversationId: String): List<String>? =
        runCatching {
            firestore.collection("conversations").document(conversationId).get().await()
                .get("participantIds") as? List<*>
        }.getOrNull()?.filterIsInstance<String>()
}
