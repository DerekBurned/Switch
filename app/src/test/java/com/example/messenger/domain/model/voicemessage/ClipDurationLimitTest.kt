package com.example.messenger.domain.model.voicemessage

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class ClipDurationLimitTest {

    @Test
    fun `video clips are capped at two minutes`() {
        assertThat(Clip.Video.MAX_DURATION_MS).isEqualTo(120_000L)
    }

    @Test
    fun `a video clip at the limit is allowed`() {
        val clip = Clip.Video(durationMs = Clip.Video.MAX_DURATION_MS)
        assertThat(clip.exceedsDurationLimit).isFalse()
    }

    @Test
    fun `a video clip past the limit is rejected`() {
        val clip = Clip.Video(durationMs = Clip.Video.MAX_DURATION_MS + 1)
        assertThat(clip.exceedsDurationLimit).isTrue()
    }

    @Test
    fun `voice clips are capped at one hour`() {
        assertThat(Clip.Voice.MAX_DURATION_MS).isEqualTo(3_600_000L)
        assertThat(Clip.Voice().maxDurationMs).isEqualTo(Clip.Voice.MAX_DURATION_MS)
    }

    @Test
    fun `a voice clip at the limit is allowed`() {
        val clip = Clip.Voice(durationMs = Clip.Voice.MAX_DURATION_MS)
        assertThat(clip.exceedsDurationLimit).isFalse()
    }

    @Test
    fun `a voice clip past the limit is rejected`() {
        val clip = Clip.Voice(durationMs = Clip.Voice.MAX_DURATION_MS + 1)
        assertThat(clip.exceedsDurationLimit).isTrue()
    }
}
