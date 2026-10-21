package com.example.messenger.data.repository

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.voicemessage.Clip
import org.junit.jupiter.api.Test

class StrandedClipsTest {

    private val startedAt = 1_000_000L

    private fun clip(
        id: String,
        status: MessageStatus,
        timestamp: Long = startedAt - 1,
        deleted: Boolean = false,
    ) = Message.Clip(id = id, status = status, timestamp = timestamp, deleted = deleted, clip = Clip.Voice())

    @Test
    fun `a clip left sending by an earlier process is stranded`() {
        val stranded = strandedClipIds(listOf(clip("a", MessageStatus.SENDING)), emptySet(), startedAt)
        assertThat(stranded).containsExactly("a")
    }

    @Test
    fun `a clip created in this process is not stranded`() {
        val fresh = clip("a", MessageStatus.SENDING, timestamp = startedAt)
        assertThat(strandedClipIds(listOf(fresh), emptySet(), startedAt)).isEmpty()
    }

    @Test
    fun `a clip with a live upload is not stranded`() {
        val uploading = clip("a", MessageStatus.SENDING)
        assertThat(strandedClipIds(listOf(uploading), setOf("a"), startedAt)).isEmpty()
    }

    @Test
    fun `failed and sent clips are not stranded`() {
        val settled = listOf(clip("f", MessageStatus.FAILED), clip("s", MessageStatus.SENT))
        assertThat(strandedClipIds(settled, emptySet(), startedAt)).isEmpty()
    }

    @Test
    fun `an outbox file without a row is dropped`() {
        assertThat(outboxFate(null)).isEqualTo(OutboxFate.DROP)
    }

    @Test
    fun `an outbox file of a deleted clip is dropped`() {
        assertThat(outboxFate(clip("a", MessageStatus.FAILED, deleted = true))).isEqualTo(OutboxFate.DROP)
    }

    @Test
    fun `unsent outbox files are kept`() {
        assertThat(outboxFate(clip("a", MessageStatus.SENDING))).isEqualTo(OutboxFate.KEEP)
        assertThat(outboxFate(clip("b", MessageStatus.FAILED))).isEqualTo(OutboxFate.KEEP)
    }

    @Test
    fun `sent outbox files are promoted to the cache`() {
        assertThat(outboxFate(clip("a", MessageStatus.SENT))).isEqualTo(OutboxFate.PROMOTE)
        assertThat(outboxFate(clip("b", MessageStatus.READ))).isEqualTo(OutboxFate.PROMOTE)
    }
}
