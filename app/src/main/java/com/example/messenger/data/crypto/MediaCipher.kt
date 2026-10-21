package com.example.messenger.data.crypto

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object MediaCipher {

    private const val NONCE_SIZE = 12
    private const val TAG_BITS = 128

    fun generateFileKey(): ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }

    fun encrypt(key: ByteArray, source: File, dest: File) {
        val nonce = ByteArray(NONCE_SIZE).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(TAG_BITS, nonce))
        dest.outputStream().use { out ->
            out.write(nonce)
            CipherOutputStream(out, cipher).use { encrypted ->
                source.inputStream().use { it.copyTo(encrypted) }
            }
        }
    }

    fun decrypt(key: ByteArray, source: File, dest: File) {
        dest.parentFile?.mkdirs()
        val part = File.createTempFile(dest.name + ".", ".part", dest.parentFile)
        try {
            source.inputStream().use { input ->
                val nonce = ByteArray(NONCE_SIZE)
                check(input.read(nonce) == NONCE_SIZE) { "encrypted file too short" }
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE,
                    SecretKeySpec(key, "AES"),
                    GCMParameterSpec(TAG_BITS, nonce))
                part.outputStream().use { out ->
                    CipherInputStream(input, cipher).use { it.copyTo(out) }
                }
            }
            Files.move(part.toPath(), dest.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (e: Exception) {
            part.delete()
            throw e
        }
    }
}
