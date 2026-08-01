package com.example.messenger.data.crypto

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.security.GeneralSecurityException

class ConversationCryptoTest {

    private val alice = ConversationCrypto.generateKeyPair()
    private val bob = ConversationCrypto.generateKeyPair()

    @Test
    fun `both sides derive the same conversation key`() {
        val aliceKey = ConversationCrypto.deriveConversationKey(
            alice.first, bob.second, "uidAlice", "uidBob", 1, 2,
        )
        val bobKey = ConversationCrypto.deriveConversationKey(
            bob.first, alice.second, "uidBob", "uidAlice", 2, 1,
        )
        assertThat(aliceKey.toList()).isEqualTo(bobKey.toList())
    }

    @Test
    fun `different epochs derive different keys`() {
        val one = ConversationCrypto.deriveConversationKey(
            alice.first, bob.second, "uidAlice", "uidBob", 1, 1,
        )
        val two = ConversationCrypto.deriveConversationKey(
            alice.first, bob.second, "uidAlice", "uidBob", 2, 1,
        )
        assertThat(one.toList()).isNotEqualTo(two.toList())
    }

    @Test
    fun `encrypt then decrypt round trips`() {
        val key = ByteArray(32) { it.toByte() }
        val aad = messageAad("c1", "m1", "s1", "TEXT")
        val blob = ConversationCrypto.encrypt(key, "hello world".toByteArray(), aad)
        val plain = ConversationCrypto.decrypt(key, blob.nonce, blob.ciphertext, aad)
        assertThat(String(plain)).isEqualTo("hello world")
    }

    @Test
    fun `tampered ciphertext fails authentication`() {
        val key = ByteArray(32) { it.toByte() }
        val aad = messageAad("c1", "m1", "s1", "TEXT")
        val blob = ConversationCrypto.encrypt(key, "hello".toByteArray(), aad)
        blob.ciphertext[0] = (blob.ciphertext[0].toInt() xor 1).toByte()
        assertThrows<GeneralSecurityException> {
            ConversationCrypto.decrypt(key, blob.nonce, blob.ciphertext, aad)
        }
    }

    @Test
    fun `mismatched aad fails authentication`() {
        val key = ByteArray(32) { it.toByte() }
        val blob = ConversationCrypto.encrypt(key, "hello".toByteArray(), messageAad("c1", "m1", "s1", "TEXT"))
        assertThrows<GeneralSecurityException> {
            ConversationCrypto.decrypt(key, blob.nonce, blob.ciphertext, messageAad("c2", "m1", "s1", "TEXT"))
        }
    }
}
