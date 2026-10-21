package com.example.messenger.data.media

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class VoiceRecordStateTest {

    @Test
    fun `a chat sees its own recording`() {
        val recording = VoiceRecordState.Recording(conversationId = "c1", elapsedMs = 1_000L)
        assertThat(recording.visibleTo("c1")).isEqualTo(recording)
    }

    @Test
    fun `another chat's recording looks idle`() {
        val recording = VoiceRecordState.Recording(conversationId = "c1", locked = true)
        assertThat(recording.visibleTo("c2")).isEqualTo(VoiceRecordState.Idle)
    }

    @Test
    fun `idle stays idle`() {
        assertThat(VoiceRecordState.Idle.visibleTo("c1")).isEqualTo(VoiceRecordState.Idle)
    }
}
