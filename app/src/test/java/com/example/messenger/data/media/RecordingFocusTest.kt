package com.example.messenger.data.media

import android.media.AudioManager
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class RecordingFocusTest {

    @Test
    fun `losing focus interrupts`() {
        assertThat(interruptsRecording(AudioManager.AUDIOFOCUS_LOSS)).isTrue()
    }

    @Test
    fun `a transient loss interrupts`() {
        assertThat(interruptsRecording(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)).isTrue()
    }

    @Test
    fun `ducking does not`() {
        assertThat(interruptsRecording(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)).isFalse()
    }

    @Test
    fun `gaining does not`() {
        assertThat(interruptsRecording(AudioManager.AUDIOFOCUS_GAIN)).isFalse()
    }
}
