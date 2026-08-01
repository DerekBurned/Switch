package com.example.messenger.data.remote.firebase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class IncomingMessagePayloadTest {

    private val base = mapOf(
        "type" to "message",
        "conversationId" to "c1",
        "messageId" to "m1",
        "senderId" to "s1",
        "preview" to "hello",
        "timestamp" to "1234",
    )

    @Test
    fun `valid payload maps to a delivered incoming text dto`() {
        val dto = parseIncomingDto(base)!!
        assertThat(dto.id).isEqualTo("m1")
        assertThat(dto.conversationId).isEqualTo("c1")
        assertThat(dto.senderId).isEqualTo("s1")
        assertThat(dto.text).isEqualTo("hello")
        assertThat(dto.timestamp).isEqualTo(1234L)
        assertThat(dto.status).isEqualTo("SENT")
        assertThat(dto.enc).isEqualTo(0)
    }

    @Test
    fun `blank conversationId yields null`() {
        assertThat(parseIncomingDto(base - "conversationId")).isNull()
    }

    @Test
    fun `blank messageId yields null`() {
        assertThat(parseIncomingDto(base - "messageId")).isNull()
    }

    @Test
    fun `missing timestamp falls back to a positive value`() {
        val dto = parseIncomingDto(base - "timestamp")!!
        assertThat(dto.timestamp).isGreaterThan(0L)
    }
}
