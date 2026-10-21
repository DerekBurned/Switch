package com.example.messenger.presentation.components.chat

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class LiveRecordingWaveformTest {

    @Test
    fun `bars that fit start at the left edge without scrolling`() {
        assertThat(liveWaveformOffset(barCount = 10, pitchPx = 5f, widthPx = 100f)).isEqualTo(0f)
    }

    @Test
    fun `once bars overflow the newest one sits at the right edge`() {
        assertThat(liveWaveformOffset(barCount = 30, pitchPx = 5f, widthPx = 100f)).isEqualTo(50f)
    }

    @Test
    fun `exactly filling the width does not scroll yet`() {
        assertThat(liveWaveformOffset(barCount = 20, pitchPx = 5f, widthPx = 100f)).isEqualTo(0f)
    }

    @Test
    fun `no bars means no offset`() {
        assertThat(liveWaveformOffset(barCount = 0, pitchPx = 5f, widthPx = 100f)).isEqualTo(0f)
    }
}
