package com.example.messenger.data.crypto

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.random.Random

class MediaCipherTest {

    @TempDir
    lateinit var dir: File

    @Test
    fun `encrypt then decrypt round trips file content`() {
        val content = Random.nextBytes(1024 * 64 + 13)
        val source = File(dir, "plain.bin").apply { writeBytes(content) }
        val encrypted = File(dir, "cipher.bin")
        val restored = File(dir, "restored.bin")
        val key = MediaCipher.generateFileKey()

        MediaCipher.encrypt(key, source, encrypted)
        MediaCipher.decrypt(key, encrypted, restored)

        assertThat(encrypted.readBytes().toList()).isNotEqualTo(content.toList())
        assertThat(restored.readBytes().toList()).isEqualTo(content.toList())
    }

    @Test
    fun `tampered file fails to decrypt`() {
        val source = File(dir, "plain.bin").apply { writeBytes(Random.nextBytes(4096)) }
        val encrypted = File(dir, "cipher.bin")
        val restored = File(dir, "restored.bin")
        val key = MediaCipher.generateFileKey()

        MediaCipher.encrypt(key, source, encrypted)
        val bytes = encrypted.readBytes()
        bytes[bytes.size / 2] = (bytes[bytes.size / 2].toInt() xor 1).toByte()
        encrypted.writeBytes(bytes)

        val result = runCatching { MediaCipher.decrypt(key, encrypted, restored) }
        assertThat(result.isSuccess && restored.exists()).isFalse()
    }
}
