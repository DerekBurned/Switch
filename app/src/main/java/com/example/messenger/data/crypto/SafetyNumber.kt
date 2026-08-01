package com.example.messenger.data.crypto

import java.math.BigInteger
import java.security.MessageDigest

object SafetyNumber {

    private const val ITERATIONS = 5200
    private val MOD = BigInteger.TEN.pow(30)

    fun compute(uidA: String, publicKeyA: ByteArray, uidB: String, publicKeyB: ByteArray): String {
        val halfA = half(uidA, publicKeyA)
        val halfB = half(uidB, publicKeyB)
        val (first, second) = if (halfA < halfB) halfA to halfB else halfB to halfA
        return (first + second).chunked(5).joinToString(" ")
    }

    private fun half(uid: String, publicKey: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-512")
        var hash = uid.toByteArray() + publicKey
        repeat(ITERATIONS) { hash = digest.digest(hash + publicKey) }
        return BigInteger(1, hash).mod(MOD).toString().padStart(30, '0')
    }
}
