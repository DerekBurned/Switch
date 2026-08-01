package com.example.messenger.data.crypto

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.matches
import org.junit.jupiter.api.Test

class SafetyNumberTest {

    private val alice = ConversationCrypto.generateKeyPair()
    private val bob = ConversationCrypto.generateKeyPair()

    @Test
    fun `is identical regardless of argument order`() {
        val fromAlice = SafetyNumber.compute("uidAlice", alice.second, "uidBob", bob.second)
        val fromBob = SafetyNumber.compute("uidBob", bob.second, "uidAlice", alice.second)
        assertThat(fromAlice).isEqualTo(fromBob)
    }

    @Test
    fun `formats as twelve groups of five digits`() {
        val number = SafetyNumber.compute("uidAlice", alice.second, "uidBob", bob.second)
        assertThat(number).matches(Regex("(\\d{5} ){11}\\d{5}"))
    }

    @Test
    fun `changes when a key changes`() {
        val before = SafetyNumber.compute("uidAlice", alice.second, "uidBob", bob.second)
        val rotated = ConversationCrypto.generateKeyPair()
        val after = SafetyNumber.compute("uidAlice", alice.second, "uidBob", rotated.second)
        assertThat(before).isNotEqualTo(after)
    }
}
