package com.example.messenger.data.crypto

import com.example.messenger.data.crypto.model.ClipSecret
import com.example.messenger.data.crypto.model.MediaSecret
import com.example.messenger.data.crypto.model.MessagePayload
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.data.remote.dto.message.RemoteMessageDto
import com.example.messenger.data.remote.dto.message.toDomain
import com.example.messenger.data.remote.dto.message.toRemoteDto
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.voicemessage.Clip
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class E2eeMessageCodec @Inject constructor(
    private val identityKeyStore: IdentityKeyStore,
    private val peerKeyRegistry: PeerKeyRegistry,
    private val authService: FirebaseAuthService,
    private val participantResolver: ParticipantResolver,
) {

    private val json = Json { ignoreUnknownKeys = true }

    // Throttles the "refresh peer key and retry" recovery below. decode() runs on every Firestore
    // snapshot emission for every message, so we must not refetch on every render of a message that
    // stays undecryptable. Keyed by "partnerId:partnerEpoch".
    private val lastKeyRefreshAttempt = java.util.concurrent.ConcurrentHashMap<String, Long>()

    suspend fun isEncryptable(conversationId: String): Boolean {
        val myUid = authService.getCurrentUserId() ?: return false
        val partnerId = participantResolver.partnerOf(conversationId, myUid) ?: return false
        return peerKeyRegistry.ensurePeerKey(partnerId) != null
    }

    suspend fun encode(message: Message): RemoteMessageDto {
        val plain = message.toRemoteDto()
        if (message is Message.Call) return plain
        val hasEncryptedMedia = when (message) {
            is Message.Media -> message.items.any { !it.fileKey.isNullOrBlank() }
            is Message.Clip -> !message.clip.fileKey.isNullOrBlank()
            else -> false
        }
        val myUid = authService.getCurrentUserId() ?: return plainOrFail(hasEncryptedMedia, plain)
        val partnerId = participantResolver.partnerOf(message.conversationId, myUid)
            ?: return plainOrFail(hasEncryptedMedia, plain)
        val peer = peerKeyRegistry.ensurePeerKey(partnerId) ?: return plainOrFail(hasEncryptedMedia, plain)
        val identity = identityKeyStore.getOrCreate(myUid)
        val payload = when (message) {
            is Message.Text -> MessagePayload(text = message.text, replyToText = message.replyToText)
            is Message.Media -> MessagePayload(
                text = message.caption,
                replyToText = message.replyToText,
                media = message.items.mapNotNull { item ->
                    item.fileKey?.takeIf { it.isNotBlank() }?.let { key ->
                        MediaSecret(itemId = item.id, key = key, blurHash = item.blurHash)
                    }
                },
            )
            is Message.Clip -> MessagePayload(
                replyToText = message.replyToText,
                clip = message.clip.toSecret(),
            )
            is Message.Call -> return plain
        }
        val key = ConversationCrypto.deriveConversationKey(
            identity.privateKey, peer.publicKey, myUid, partnerId, identity.epoch, peer.epoch,
        )
        val aad = messageAad(plain.conversationId, plain.id, plain.senderId, plain.type)
        val blob = ConversationCrypto.encrypt(key, json.encodeToString(payload).toByteArray(), aad)
        return plain.copy(
            text = "",
            replyToText = null,
            mediaItems = plain.mediaItems.map { it.copy(blurHash = "") },
            clip = plain.clip?.copy(waveform = emptyList()),
            enc = 1,
            ciphertext = b64(blob.ciphertext),
            nonce = b64(blob.nonce),
            senderEpoch = identity.epoch,
            recipientEpoch = peer.epoch,
            recipientId = partnerId,
        )
    }

    private fun plainOrFail(hasEncryptedMedia: Boolean, plain: RemoteMessageDto): RemoteMessageDto {
        check(!hasEncryptedMedia) {
            "media already encrypted but no peer key resolvable; refusing to send stranded plaintext"
        }
        return plain
    }

    suspend fun decode(dto: RemoteMessageDto, hasPendingWrites: Boolean = false): Message {
        if (dto.enc != 1) return dto.toDomain(hasPendingWrites)
        val payload = runCatching { decryptPayload(dto) }.getOrNull()
            ?: runCatching { retryWithFreshPeerKey(dto) }.getOrNull()
            ?: return undecryptable(dto, hasPendingWrites)
        val message = dto.copy(text = payload.text, replyToText = payload.replyToText)
            .toDomain(hasPendingWrites)
        val clipSecret = payload.clip
        return when {
            message is Message.Media && payload.media.isNotEmpty() -> {
                val secrets = payload.media.associateBy { it.itemId }
                message.copy(
                    items = message.items.map { item ->
                        secrets[item.id]?.let { secret ->
                            item.copy(fileKey = secret.key, blurHash = secret.blurHash)
                        } ?: item
                    },
                )
            }
            message is Message.Clip && clipSecret != null ->
                message.copy(clip = message.clip.withSecret(clipSecret))
            else -> message
        }
    }

    private fun Clip.toSecret(): ClipSecret = when (this) {
        is Clip.Voice -> ClipSecret(key = fileKey?.takeIf { it.isNotBlank() }, waveform = waveform)
        is Clip.Video -> ClipSecret(key = fileKey?.takeIf { it.isNotBlank() })
    }

    private fun Clip.withSecret(secret: ClipSecret): Clip = when (this) {
        is Clip.Voice -> copy(fileKey = secret.key, waveform = secret.waveform)
        is Clip.Video -> copy(fileKey = secret.key)
    }

    private suspend fun decryptPayload(dto: RemoteMessageDto): MessagePayload {
        val myUid = authService.getCurrentUserId() ?: error("not signed in")
        val iAmSender = dto.senderId == myUid
        val partnerId = if (iAmSender) dto.recipientId else dto.senderId
        val myEpoch = if (iAmSender) dto.senderEpoch else dto.recipientEpoch
        val partnerEpoch = if (iAmSender) dto.recipientEpoch else dto.senderEpoch
        val identity = identityKeyStore.getOrCreate(myUid)
        check(identity.epoch == myEpoch) { "local epoch ${identity.epoch}, message needs $myEpoch" }
        val peer = peerKeyRegistry.cachedPeerKey(partnerId, partnerEpoch)
            ?: peerKeyRegistry.peerKeyForEpoch(partnerId, partnerEpoch)
            ?: error("no key for $partnerId epoch $partnerEpoch")
        val key = ConversationCrypto.deriveConversationKey(
            identity.privateKey, peer.publicKey, myUid, partnerId, identity.epoch, peer.epoch,
        )
        val aad = messageAad(dto.conversationId, dto.id, dto.senderId, dto.type)
        val plaintext = ConversationCrypto.decrypt(key, unb64(dto.nonce), unb64(dto.ciphertext), aad)
        return json.decodeFromString(String(plaintext))
    }

    /**
     * Last-resort recovery for a message that failed to decrypt with the locally cached peer key:
     * the peer likely regenerated their identity and republished, so refetch their published key
     * for the needed epoch and try once more. Throttled per peer/epoch so repeated renders of a
     * genuinely unrecoverable message don't repeatedly hit Firestore.
     */
    private suspend fun retryWithFreshPeerKey(dto: RemoteMessageDto): MessagePayload {
        val myUid = authService.getCurrentUserId() ?: error("not signed in")
        val iAmSender = dto.senderId == myUid
        val partnerId = if (iAmSender) dto.recipientId else dto.senderId
        val partnerEpoch = if (iAmSender) dto.recipientEpoch else dto.senderEpoch
        check(partnerId.isNotBlank()) { "no partner to refresh key for" }
        val throttleKey = "$partnerId:$partnerEpoch"
        val now = System.currentTimeMillis()
        val last = lastKeyRefreshAttempt[throttleKey]
        check(last == null || now - last >= KEY_REFRESH_THROTTLE_MS) { "peer key recently refreshed" }
        lastKeyRefreshAttempt[throttleKey] = now
        peerKeyRegistry.refreshPeerKey(partnerId, partnerEpoch)
        return decryptPayload(dto)
    }

    private fun undecryptable(dto: RemoteMessageDto, hasPendingWrites: Boolean): Message =
        dto.copy(
            text = UNDECRYPTABLE_TEXT,
            type = Message.TYPE_TEXT,
            mediaItems = emptyList(),
            clip = null,
            replyToMessageId = null,
            replyToText = null,
            replyToSenderId = null,
        ).toDomain(hasPendingWrites)

    companion object {
        const val UNDECRYPTABLE_TEXT = "Message cannot be decrypted on this device"
        private const val KEY_REFRESH_THROTTLE_MS = 5 * 60 * 1000L
    }
}
