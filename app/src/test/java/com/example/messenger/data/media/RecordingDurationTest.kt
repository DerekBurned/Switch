package com.example.messenger.data.media

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import com.example.messenger.domain.model.voicemessage.Clip
import org.junit.jupiter.api.Test

class RecordingDurationTest {

    @Test
    fun `duration spans start to stop`() {
        assertThat(recordedDurationMs(startedAt = 1_000L, stoppedAt = 4_500L)).isEqualTo(3_500L)
    }

    @Test
    fun `a clock going backwards yields zero`() {
        assertThat(recordedDurationMs(startedAt = 5_000L, stoppedAt = 4_000L)).isEqualTo(0L)
    }

    @Test
    fun `an overshoot is clamped to the cap`() {
        val overshoot = recordedDurationMs(startedAt = 0L, stoppedAt = Clip.Voice.MAX_DURATION_MS + 250L)
        assertThat(overshoot).isEqualTo(Clip.Voice.MAX_DURATION_MS)
    }

    @Test
    fun `a clamped recording is still accepted`() {
        val durationMs = recordedDurationMs(startedAt = 0L, stoppedAt = Clip.Voice.MAX_DURATION_MS + 250L)
        assertThat(Clip.Voice(durationMs = durationMs).exceedsDurationLimit).isFalse()
    }
}
