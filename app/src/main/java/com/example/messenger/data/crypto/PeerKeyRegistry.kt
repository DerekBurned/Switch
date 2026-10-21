package com.example.messenger.data.crypto

import com.example.messenger.data.crypto.model.PeerKey
import com.example.messenger.data.local.obx.ObxPeerKey
import com.example.messenger.data.local.obx.ObxPeerKey_
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import io.objectbox.Box
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeerKeyRegistry @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val peerKeyBox: Box<ObxPeerKey>,
    private val identityKeyStore: IdentityKeyStore,
) {

    suspend fun publishIfNeeded(myUid: String) {
        var identity = identityKeyStore.getOrCreate(myUid)
        var localPub = b64(identity.publicKey)
        val existing = runCatching {
            keysCollection(myUid).document(identity.epoch.toString()).get().await()
        }.getOrNull()

        // A regenerated identity restarts at epoch 1 and would otherwise overwrite the public key a
        // peer still has cached, permanently breaking decryption of anything sent to the old key.
        // If this epoch is already published with a different key, rotate onto a fresh epoch above
        // the server's max instead of clobbering history.
        if (existing?.exists() == true && existing.getString("pub") != localPub) {
            val serverMax = runCatching {
                keysCollection(myUid).orderBy("epoch", Query.Direction.DESCENDING).limit(1).get()
                    .await().documents.firstOrNull()?.getLong("epoch")?.toInt()
            }.getOrNull() ?: identity.epoch
            while (identity.epoch <= serverMax) identity = identityKeyStore.rotate(myUid)
            localPub = b64(identity.publicKey)
        } else if (existing?.getString("pub") == localPub) {
            return
        }

        keysCollection(myUid).document(identity.epoch.toString()).set(
            mapOf(
                "pub" to localPub,
                "epoch" to identity.epoch,
                "algo" to "X25519",
                "updatedAt" to System.currentTimeMillis(),
            ),
        ).await()
    }

    /**
     * Force-fetches the peer's published key for [epoch] from Firestore, bypassing the local cache,
     * and upserts it. Used to recover when a locally cached peer key has gone stale (e.g. the peer
     * regenerated their identity and republished under the same epoch number).
     */
    suspend fun refreshPeerKey(uid: String, epoch: Int): PeerKey? {
        val doc = runCatching { keysCollection(uid).document(epoch.toString()).get().await() }
            .getOrNull() ?: return cachedPeerKey(uid, epoch)
        val pub = doc.getString("pub") ?: return cachedPeerKey(uid, epoch)
        return store(uid, epoch, pub).toPeerKey()
    }

    suspend fun ensurePeerKey(uid: String): PeerKey? {
        val cached = latestLocal(uid)
        if (cached != null && System.currentTimeMillis() - cached.seenAt < REFRESH_MS) {
            return cached.toPeerKey()
        }
        val snapshot = runCatching {
            keysCollection(uid).orderBy("epoch", Query.Direction.DESCENDING).limit(1).get().await()
        }.getOrNull()
        val doc = snapshot?.documents?.firstOrNull() ?: return cached?.toPeerKey()
        val pub = doc.getString("pub") ?: return cached?.toPeerKey()
        val epoch = doc.getLong("epoch")?.toInt() ?: return cached?.toPeerKey()
        return store(uid, epoch, pub).toPeerKey()
    }

    suspend fun peerKeyForEpoch(uid: String, epoch: Int): PeerKey? {
        localRow(uid, epoch)?.let { return it.toPeerKey() }
        val doc = runCatching { keysCollection(uid).document(epoch.toString()).get().await() }
            .getOrNull() ?: return null
        val pub = doc.getString("pub") ?: return null
        return store(uid, epoch, pub).toPeerKey()
    }

    fun cachedPeerKey(uid: String, epoch: Int): PeerKey? = localRow(uid, epoch)?.toPeerKey()

    fun latestPeerKey(uid: String): PeerKey? = latestLocal(uid)?.toPeerKey()

    fun setVerified(uid: String, verified: Boolean) {
        val row = latestLocal(uid) ?: return
        row.verified = verified
        peerKeyBox.put(row)
    }

    private fun store(uid: String, epoch: Int, pub: String): ObxPeerKey {
        val existing = localRow(uid, epoch)
        // A changed public key under the same epoch means the key was regenerated: any prior
        // manual verification no longer applies to this new key.
        val stillVerified = existing?.verified == true && existing.publicKey == pub
        val row = ObxPeerKey(
            boxId = existing?.boxId ?: 0,
            uidEpoch = "$uid:$epoch",
            uid = uid,
            epoch = epoch,
            publicKey = pub,
            verified = stillVerified,
            seenAt = System.currentTimeMillis(),
        )
        peerKeyBox.put(row)
        return row
    }

    private fun localRow(uid: String, epoch: Int): ObxPeerKey? =
        peerKeyBox.query(ObxPeerKey_.uidEpoch.equal("$uid:$epoch")).build().use { it.findFirst() }

    private fun latestLocal(uid: String): ObxPeerKey? =
        peerKeyBox.query(ObxPeerKey_.uid.equal(uid)).orderDesc(ObxPeerKey_.epoch).build()
            .use { it.findFirst() }

    private fun keysCollection(uid: String) =
        firestore.collection("users").document(uid).collection("keys")

    private fun ObxPeerKey.toPeerKey() = PeerKey(uid, unb64(publicKey), epoch, verified)

    companion object {
        private const val REFRESH_MS = 30 * 60 * 1000L
    }
}
