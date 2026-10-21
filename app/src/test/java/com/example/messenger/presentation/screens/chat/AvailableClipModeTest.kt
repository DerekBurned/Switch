package com.example.messenger.presentation.screens.chat

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.example.messenger.domain.model.voicemessage.ClipKind
import org.junit.jupiter.api.Test

class AvailableClipModeTest {

    @Test
    fun `keeps the preferred mode while both kinds are on`() {
        assertThat(availableClipMode(ClipKind.VIDEO, voiceEnabled = true, videoNotesEnabled = true))
            .isEqualTo(ClipKind.VIDEO)
        assertThat(availableClipMode(ClipKind.VOICE, voiceEnabled = true, videoNotesEnabled = true))
            .isEqualTo(ClipKind.VOICE)
    }

    @Test
    fun `falls back to voice when video notes are off`() {
        assertThat(availableClipMode(ClipKind.VIDEO, voiceEnabled = true, videoNotesEnabled = false))
            .isEqualTo(ClipKind.VOICE)
    }

    @Test
    fun `falls back to video notes when voice is off`() {
        assertThat(availableClipMode(ClipKind.VOICE, voiceEnabled = false, videoNotesEnabled = true))
            .isEqualTo(ClipKind.VIDEO)
    }

    @Test
    fun `no recording mode when both kinds are off`() {
        assertThat(availableClipMode(ClipKind.VOICE, voiceEnabled = false, videoNotesEnabled = false)).isNull()
    }
}
