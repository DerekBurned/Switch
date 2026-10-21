package com.example.messenger.data.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ClipUploadTimeoutTest {

    @Test
    fun `an empty file gets the base allowance`() {
        assertThat(clipUploadTimeout(0L)).isEqualTo(30.seconds)
    }

    @Test
    fun `every 8000 bytes adds a second`() {
        assertThat(clipUploadTimeout(16_000L)).isEqualTo(32.seconds)
    }

    @Test
    fun `a one hour voice note gets more than an hour`() {
        val oneHourAt64Kbps = 64_000L / 8 * 60 * 60
        assertThat(clipUploadTimeout(oneHourAt64Kbps)).isGreaterThan(60.minutes)
    }

    @Test
    fun `bigger files never get less time`() {
        val sizes = listOf(0L, 1L, 7_999L, 8_000L, 1_000_000L, 28_800_000L)
        sizes.zipWithNext().forEach { (smaller, bigger) ->
            assertThat(clipUploadTimeout(bigger)).isGreaterThanOrEqualTo(clipUploadTimeout(smaller))
        }
    }

    @Test
    fun `negative sizes count as empty`() {
        assertThat(clipUploadTimeout(-5L)).isEqualTo(clipUploadTimeout(0L))
    }
}
