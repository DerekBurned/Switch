package com.example.messenger.data.crypto

import com.google.crypto.tink.subtle.Hkdf
import com.google.crypto.tink.subtle.X25519
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptedBlob(val nonce: ByteArray, val ciphertext: ByteArray)

object ConversationCrypto {

    const val NONCE_SIZE = 12
    private const val TAG_BITS = 128
    private const val KEY_SIZE = 32
    private const val INFO_PREFIX = "msgr-e2ee-v1"

    fun generateKeyPair(): Pair<ByteArray, ByteArray> {
        val privateKey = X25519.generatePrivateKey()
        return privateKey to X25519.publicFromPrivate(privateKey)
    }

    fun deriveConversationKey(
        myPrivateKey: ByteArray,
        peerPublicKey: ByteArray,
        myUid: String,
        peerUid: String,
        myEpoch: Int,
        peerEpoch: Int,
    ): ByteArray {
        val shared = X25519.computeSharedSecret(myPrivateKey, peerPublicKey)
        val (loUid, hiUid) = if (myUid < peerUid) myUid to peerUid else peerUid to myUid
        val (loEpoch, hiEpoch) = if (myUid < peerUid) myEpoch to peerEpoch else peerEpoch to myEpoch
        val salt = MessageDigest.getInstance("SHA-256").digest("$loUid|$hiUid".toByteArray())
        return Hkdf.computeHkdf(
            "HMACSHA256",
            shared,
            salt,
            "$INFO_PREFIX|$loEpoch|$hiEpoch".toByteArray(),
            KEY_SIZE,
        )
    }

    fun encrypt(key: ByteArray, plaintext: ByteArray, aad: ByteArray): EncryptedBlob {
        val nonce = ByteArray(NONCE_SIZE).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad)
        return EncryptedBlob(nonce, cipher.doFinal(plaintext))
    }

    fun decrypt(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray, aad: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(ciphertext)
    }
}
