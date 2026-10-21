package com.example.messenger.data.local.obx

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class ClipConverterTest {

    private val converter = ClipConverter()

    @Test
    fun `payload survives the json round trip`() {
        val payload = ClipPayload(
            url = "https://example/clip.m4a",
            durationMs = 2500L,
            sizeBytes = 128L,
            fileKey = "key",
            waveform = listOf(4, 8, 15, 16),
        )
        val restored = converter.convertToEntityProperty(converter.convertToDatabaseValue(payload))
        assertThat(restored).isEqualTo(payload)
    }

    @Test
    fun `empty waveform survives the json round trip`() {
        val payload = ClipPayload(url = "https://example/clip.mp4", durationMs = 10L)
        val restored = converter.convertToEntityProperty(converter.convertToDatabaseValue(payload))
        assertThat(restored).isEqualTo(payload)
    }

    @Test
    fun `null payload maps to null column`() {
        assertThat(converter.convertToDatabaseValue(null)).isNull()
    }

    @Test
    fun `blank column maps to null payload`() {
        assertThat(converter.convertToEntityProperty("")).isNull()
        assertThat(converter.convertToEntityProperty(null)).isNull()
    }

    @Test
    fun `corrupt column maps to null rather than throwing`() {
        assertThat(converter.convertToEntityProperty("not json at all")).isNull()
    }
}
