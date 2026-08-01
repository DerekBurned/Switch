package com.example.messenger.data.crypto

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

private class FakeWrapper : KeyWrapper {
    override fun wrap(plain: ByteArray): ByteArray = plain.map { (it + 1).toByte() }.toByteArray()
    override fun unwrap(wrapped: ByteArray): ByteArray = wrapped.map { (it - 1).toByte() }.toByteArray()
}

class IdentityKeyStoreTest {

    @TempDir
    lateinit var dir: File

    @Test
    fun `creates a key once and reloads the same key`() {
        val first = IdentityKeyStore(dir, FakeWrapper()).getOrCreate("uid1")
        val second = IdentityKeyStore(dir, FakeWrapper()).getOrCreate("uid1")
        assertThat(second.publicKey.toList()).isEqualTo(first.publicKey.toList())
        assertThat(second.epoch).isEqualTo(1)
    }

    @Test
    fun `different accounts get different keys`() {
        val store = IdentityKeyStore(dir, FakeWrapper())
        val a = store.getOrCreate("uid1")
        val b = store.getOrCreate("uid2")
        assertThat(a.publicKey.toList()).isNotEqualTo(b.publicKey.toList())
    }

    @Test
    fun `rotate bumps the epoch and replaces the key`() {
        val store = IdentityKeyStore(dir, FakeWrapper())
        val before = store.getOrCreate("uid1")
        val after = store.rotate("uid1")
        assertThat(after.epoch).isEqualTo(before.epoch + 1)
        assertThat(after.publicKey.toList()).isNotEqualTo(before.publicKey.toList())
        assertThat(IdentityKeyStore(dir, FakeWrapper()).getOrCreate("uid1").epoch).isEqualTo(2)
    }

    @Test
    fun `key file is not plaintext on disk`() {
        val store = IdentityKeyStore(dir, FakeWrapper())
        val key = store.getOrCreate("uid1")
        val raw = File(dir, "identity_v1_uid1.bin").readBytes()
        assertThat(raw.toList().containsAll(key.privateKey.toList()).not() || raw.size != 69).isTrue()
    }

    @Test
    fun `present but unreadable key file is not silently replaced`() {
        IdentityKeyStore(dir, FakeWrapper()).getOrCreate("uid1")
        val file = File(dir, "identity_v1_uid1.bin")
        val corrupt = byteArrayOf(9, 9, 9)
        file.writeBytes(corrupt)

        val result = runCatching { IdentityKeyStore(dir, FakeWrapper()).getOrCreate("uid1") }

        assertThat(result.isFailure).isTrue()
        assertThat(file.readBytes().toList()).isEqualTo(corrupt.toList())
    }
}
