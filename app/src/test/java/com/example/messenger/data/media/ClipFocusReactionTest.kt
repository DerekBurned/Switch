package com.example.messenger.data.media

import android.media.AudioManager
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class ClipFocusReactionTest {

    @Test
    fun `a permanent loss pauses and releases focus`() {
        assertThat(focusReaction(AudioManager.AUDIOFOCUS_LOSS, resumeOnGain = true))
            .isEqualTo(FocusReaction.PAUSE_AND_RELEASE)
    }

    @Test
    fun `a transient loss pauses and keeps focus`() {
        assertThat(focusReaction(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, resumeOnGain = false))
            .isEqualTo(FocusReaction.PAUSE_AND_HOLD)
    }

    @Test
    fun `a duck request pauses speech instead of ducking`() {
        assertThat(focusReaction(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK, resumeOnGain = false))
            .isEqualTo(FocusReaction.PAUSE_AND_HOLD)
    }

    @Test
    fun `regaining focus resumes what a transient loss paused`() {
        assertThat(focusReaction(AudioManager.AUDIOFOCUS_GAIN, resumeOnGain = true))
            .isEqualTo(FocusReaction.RESUME)
    }

    @Test
    fun `regaining focus after a user pause stays paused`() {
        assertThat(focusReaction(AudioManager.AUDIOFOCUS_GAIN, resumeOnGain = false))
            .isEqualTo(FocusReaction.NONE)
    }

    @Test
    fun `unknown changes are ignored`() {
        assertThat(focusReaction(12_345, resumeOnGain = true)).isEqualTo(FocusReaction.NONE)
    }
}
