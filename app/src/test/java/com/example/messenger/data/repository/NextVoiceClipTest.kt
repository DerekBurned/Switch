package com.example.messenger.data.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.voicemessage.Clip
import org.junit.jupiter.api.Test

class NextVoiceClipTest {

    private fun voice(
        id: String,
        timestamp: Long,
        sender: String = "peer",
        played: Boolean = false,
        deleted: Boolean = false,
    ) = Message.Clip(
        id = id,
        conversationId = "c1",
        senderId = sender,
        timestamp = timestamp,
        played = played,
        deleted = deleted,
        clip = Clip.Voice(),
    )

    private val current = voice("m5", 500)

    @Test
    fun `the following voice clip plays next`() {
        val candidates = listOf(current, voice("m6", 600), voice("m7", 700))
        assertThat(nextVoiceClipAfter(current, candidates)?.id).isEqualTo("m6")
    }

    @Test
    fun `own and already played clips are included`() {
        val mine = voice("m6", 600, sender = "me", played = true)
        assertThat(nextVoiceClipAfter(current, listOf(current, mine))?.id).isEqualTo("m6")
    }

    @Test
    fun `video clips and other messages are skipped`() {
        val candidates = listOf(
            Message.Clip(id = "v6", conversationId = "c1", timestamp = 600, clip = Clip.Video()),
            Message.Text(id = "t7", conversationId = "c1", timestamp = 700, text = "hi"),
            voice("m8", 800),
        )
        assertThat(nextVoiceClipAfter(current, candidates)?.id).isEqualTo("m8")
    }

    @Test
    fun `deleted clips are skipped`() {
        val candidates = listOf(voice("m6", 600, deleted = true), voice("m7", 700))
        assertThat(nextVoiceClipAfter(current, candidates)?.id).isEqualTo("m7")
    }

    @Test
    fun `older clips are ignored`() {
        val candidates = listOf(voice("m1", 100), voice("m4", 400))
        assertThat(nextVoiceClipAfter(current, candidates)).isNull()
    }

    @Test
    fun `equal timestamps are ordered by id`() {
        val candidates = listOf(voice("m5b", 500), voice("m4z", 500))
        assertThat(nextVoiceClipAfter(current, candidates)?.id).isEqualTo("m5b")
    }

    @Test
    fun `unsorted candidates still yield the earliest following clip`() {
        val candidates = listOf(voice("m9", 900), voice("m6", 600), voice("m7", 700))
        assertThat(nextVoiceClipAfter(current, candidates)?.id).isEqualTo("m6")
    }
}
