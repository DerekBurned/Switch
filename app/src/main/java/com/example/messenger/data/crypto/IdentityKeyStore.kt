package com.example.messenger.data.crypto

import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

class IdentityKey(val privateKey: ByteArray, val publicKey: ByteArray, val epoch: Int)

class IdentityKeyStore(
    private val baseDir: File,
    private val keyWrapper: KeyWrapper,
) {

    private val cache = ConcurrentHashMap<String, IdentityKey>()

    fun getOrCreate(uid: String): IdentityKey = cache.computeIfAbsent(uid) {
        if (keyFile(uid).exists()) load(uid) else generateAndPersist(uid, epoch = 1)
    }

    fun rotate(uid: String): IdentityKey {
        val next = generateAndPersist(uid, epoch = getOrCreate(uid).epoch + 1)
        cache[uid] = next
        return next
    }

    private fun load(uid: String): IdentityKey {
        val bytes = keyWrapper.unwrap(keyFile(uid).readBytes())
        return decode(bytes) ?: error("identity key file for $uid is present but unreadable")
    }

    private fun generateAndPersist(uid: String, epoch: Int): IdentityKey {
        val (privateKey, publicKey) = ConversationCrypto.generateKeyPair()
        val buffer = ByteBuffer.allocate(69)
        buffer.put(1)
        buffer.putInt(epoch)
        buffer.put(privateKey)
        buffer.put(publicKey)
        baseDir.mkdirs()
        keyFile(uid).writeBytes(keyWrapper.wrap(buffer.array()))
        return IdentityKey(privateKey, publicKey, epoch)
    }

    private fun decode(bytes: ByteArray): IdentityKey? {
        if (bytes.size != 69 || bytes[0].toInt() != 1) return null
        val buffer = ByteBuffer.wrap(bytes, 1, 68)
        val epoch = buffer.int
        val privateKey = ByteArray(32).also { buffer.get(it) }
        val publicKey = ByteArray(32).also { buffer.get(it) }
        return IdentityKey(privateKey, publicKey, epoch)
    }

    private fun keyFile(uid: String) = File(baseDir, "identity_v1_$uid.bin")
}
