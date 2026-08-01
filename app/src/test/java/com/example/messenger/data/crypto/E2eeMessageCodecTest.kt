package com.example.messenger.data.crypto

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.domain.model.CallType
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.Message
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class E2eeMessageCodecTest {

    private val aliceKeys = ConversationCrypto.generateKeyPair()
    private val bobKeys = ConversationCrypto.generateKeyPair()
    private val aliceIdentity = IdentityKey(aliceKeys.first, aliceKeys.second, 1)
    private val bobIdentity = IdentityKey(bobKeys.first, bobKeys.second, 1)

    private fun codecFor(
        myUid: String,
        myIdentity: IdentityKey,
        peerUid: String,
        peerIdentity: IdentityKey,
    ): E2eeMessageCodec {
        val identityStore = mockk<IdentityKeyStore> {
            every { getOrCreate(myUid) } returns myIdentity
        }
        val peer = PeerKey(peerUid, peerIdentity.publicKey, peerIdentity.epoch, false)
        val registry = mockk<PeerKeyRegistry> {
            coEvery { ensurePeerKey(peerUid) } returns peer
            every { cachedPeerKey(peerUid, peerIdentity.epoch) } returns peer
            coEvery { peerKeyForEpoch(peerUid, peerIdentity.epoch) } returns peer
            coEvery { refreshPeerKey(peerUid, peerIdentity.epoch) } returns peer
        }
        val auth = mockk<FirebaseAuthService> {
            every { getCurrentUserId() } returns myUid
        }
        return E2eeMessageCodec(identityStore, registry, auth, { _, _ -> peerUid })
    }

    private val original = Message.Text(
        id = "m1",
        conversationId = "c1",
        senderId = "alice",
        timestamp = 42L,
        text = "secret text",
        replyToText = "earlier message",
        replyToMessageId = "m0",
    )

    @Test
    fun `encode hides plaintext and decode on the other side restores it`() = runTest {
        val aliceCodec = codecFor("alice", aliceIdentity, "bob", bobIdentity)
        val dto = aliceCodec.encode(original)

        assertThat(dto.enc).isEqualTo(1)
        assertThat(dto.text).isEmpty()
        assertThat(dto.replyToText ?: "").isEmpty()
        assertThat(dto.ciphertext).isNotEmpty()

        val bobCodec = codecFor("bob", bobIdentity, "alice", aliceIdentity)
        val decoded = bobCodec.decode(dto)

        assertThat(decoded).isInstanceOf(Message.Text::class)
        assertThat((decoded as Message.Text).text).isEqualTo("secret text")
        assertThat(decoded.replyToText).isEqualTo("earlier message")
        assertThat(decoded.replyToMessageId).isEqualTo("m0")
    }

    @Test
    fun `sender can decode their own echoed message`() = runTest {
        val aliceCodec = codecFor("alice", aliceIdentity, "bob", bobIdentity)
        val dto = aliceCodec.encode(original)
        val decoded = aliceCodec.decode(dto) as Message.Text
        assertThat(decoded.text).isEqualTo("secret text")
    }

    @Test
    fun `tampered envelope decodes to a placeholder instead of crashing`() = runTest {
        val aliceCodec = codecFor("alice", aliceIdentity, "bob", bobIdentity)
        val dto = aliceCodec.encode(original)
        val corrupted = dto.copy(ciphertext = dto.ciphertext.dropLast(4) + "AAAA")
        val bobCodec = codecFor("bob", bobIdentity, "alice", aliceIdentity)
        val decoded = bobCodec.decode(corrupted) as Message.Text
        assertThat(decoded.text).isEqualTo(E2eeMessageCodec.UNDECRYPTABLE_TEXT)
    }

    @Test
    fun `stale cached peer key is refreshed and the message decodes`() = runTest {
        // Alice sends a message encrypted to Bob's real key.
        val aliceCodec = codecFor("alice", aliceIdentity, "bob", bobIdentity)
        val dto = aliceCodec.encode(original)

        // Bob's device holds a WRONG (stale) public key for Alice, as if Alice regenerated her
        // identity and republished. refreshPeerKey fetches the correct key and updates the cache,
        // exactly as the ObjectBox-backed registry does in production.
        val wrongKeys = ConversationCrypto.generateKeyPair()
        val stalePeer = PeerKey("alice", wrongKeys.second, aliceIdentity.epoch, false)
        val freshPeer = PeerKey("alice", aliceIdentity.publicKey, aliceIdentity.epoch, false)
        var bobViewOfAlice = stalePeer

        val identityStore = mockk<IdentityKeyStore> {
            every { getOrCreate("bob") } returns bobIdentity
        }
        val registry = mockk<PeerKeyRegistry> {
            every { cachedPeerKey("alice", aliceIdentity.epoch) } answers { bobViewOfAlice }
            coEvery { peerKeyForEpoch("alice", aliceIdentity.epoch) } answers { bobViewOfAlice }
            coEvery { refreshPeerKey("alice", aliceIdentity.epoch) } answers {
                freshPeer.also { bobViewOfAlice = it }
            }
        }
        val auth = mockk<FirebaseAuthService> {
            every { getCurrentUserId() } returns "bob"
        }
        val bobCodec = E2eeMessageCodec(identityStore, registry, auth, { _, _ -> "alice" })

        val decoded = bobCodec.decode(dto) as Message.Text
        assertThat(decoded.text).isEqualTo("secret text")
    }

    @Test
    fun `call messages pass through unencrypted`() = runTest {
        val aliceCodec = codecFor("alice", aliceIdentity, "bob", bobIdentity)
        val call = Message.Call(
            id = "m2", conversationId = "c1", senderId = "alice",
            timestamp = 42L, callType = CallType.MISSED,
        )
        val dto = aliceCodec.encode(call)
        assertThat(dto.enc).isEqualTo(0)
    }

    @Test
    fun `missing peer key falls back to plaintext`() = runTest {
        val identityStore = mockk<IdentityKeyStore> {
            every { getOrCreate("alice") } returns aliceIdentity
        }
        val registry = mockk<PeerKeyRegistry> {
            coEvery { ensurePeerKey("bob") } returns null
        }
        val auth = mockk<FirebaseAuthService> {
            every { getCurrentUserId() } returns "alice"
        }
        val codec = E2eeMessageCodec(identityStore, registry, auth, { _, _ -> "bob" })
        val dto = codec.encode(original)
        assertThat(dto.enc).isEqualTo(0)
        assertThat(dto.text).isEqualTo("secret text")
    }

    @Test
    fun `plaintext media with no peer key still falls back to plaintext`() = runTest {
        val identityStore = mockk<IdentityKeyStore> {
            every { getOrCreate("alice") } returns aliceIdentity
        }
        val registry = mockk<PeerKeyRegistry> {
            coEvery { ensurePeerKey("bob") } returns null
        }
        val auth = mockk<FirebaseAuthService> {
            every { getCurrentUserId() } returns "alice"
        }
        val codec = E2eeMessageCodec(identityStore, registry, auth, { _, _ -> "bob" })
        val media = Message.Media(
            id = "m3", conversationId = "c1", senderId = "alice", timestamp = 1L,
            items = listOf(MediaItem(id = "i1")),
            caption = "hi",
        )
        val dto = codec.encode(media)
        assertThat(dto.enc).isEqualTo(0)
    }

    @Test
    fun `already-encrypted media with no peer key fails instead of stranding plaintext`() = runTest {
        val identityStore = mockk<IdentityKeyStore> {
            every { getOrCreate("alice") } returns aliceIdentity
        }
        val registry = mockk<PeerKeyRegistry> {
            coEvery { ensurePeerKey("bob") } returns null
        }
        val auth = mockk<FirebaseAuthService> {
            every { getCurrentUserId() } returns "alice"
        }
        val codec = E2eeMessageCodec(identityStore, registry, auth, { _, _ -> "bob" })
        val media = Message.Media(
            id = "m4", conversationId = "c1", senderId = "alice", timestamp = 1L,
            items = listOf(MediaItem(id = "i1", fileKey = "AAAAAAAAAAAAAAAAAAAAAA==")),
            caption = "hi",
        )
        val result = runCatching { codec.encode(media) }
        assertThat(result.isFailure).isTrue()
    }
}
