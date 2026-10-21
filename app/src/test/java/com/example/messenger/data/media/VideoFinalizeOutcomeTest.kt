package com.example.messenger.data.media

import androidx.camera.video.VideoRecordEvent
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class VideoFinalizeOutcomeTest {

    private val ok = VideoRecordEvent.Finalize.ERROR_NONE

    @Test
    fun `a stopped recording with a file is sent`() {
        assertThat(videoFinalizeOutcome(VideoEnd.SEND, ok, fileBytes = 50_000, durationMs = 3_000))
            .isEqualTo(VideoEnd.SEND)
    }

    @Test
    fun `a cancelled recording is always discarded`() {
        assertThat(videoFinalizeOutcome(VideoEnd.DISCARD, ok, fileBytes = 50_000, durationMs = 3_000))
            .isEqualTo(VideoEnd.DISCARD)
    }

    @Test
    fun `hitting the duration limit while still holding becomes a draft`() {
        val limit = VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED
        assertThat(videoFinalizeOutcome(null, limit, fileBytes = 50_000, durationMs = 120_000))
            .isEqualTo(VideoEnd.DRAFT)
    }

    @Test
    fun `the camera closing underneath keeps what was recorded as a draft`() {
        val inactive = VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE
        assertThat(videoFinalizeOutcome(null, inactive, fileBytes = 50_000, durationMs = 4_000))
            .isEqualTo(VideoEnd.DRAFT)
    }

    @Test
    fun `hitting the file size limit while still holding becomes a draft`() {
        val limit = VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED
        assertThat(videoFinalizeOutcome(null, limit, fileBytes = 38_000_000, durationMs = 90_000))
            .isEqualTo(VideoEnd.DRAFT)
    }

    @Test
    fun `an interrupted recording becomes a draft`() {
        assertThat(videoFinalizeOutcome(VideoEnd.DRAFT, ok, fileBytes = 50_000, durationMs = 4_000))
            .isEqualTo(VideoEnd.DRAFT)
    }

    @Test
    fun `an encoder failure or an empty file is discarded`() {
        val encoder = VideoRecordEvent.Finalize.ERROR_ENCODING_FAILED
        assertThat(videoFinalizeOutcome(VideoEnd.SEND, encoder, fileBytes = 50_000, durationMs = 3_000))
            .isEqualTo(VideoEnd.DISCARD)
        assertThat(videoFinalizeOutcome(VideoEnd.SEND, ok, fileBytes = 0, durationMs = 3_000))
            .isEqualTo(VideoEnd.DISCARD)
    }

    @Test
    fun `a recording shorter than a second is discarded`() {
        assertThat(videoFinalizeOutcome(VideoEnd.SEND, ok, fileBytes = 50_000, durationMs = 600))
            .isEqualTo(VideoEnd.DISCARD)
        assertThat(videoFinalizeOutcome(VideoEnd.SEND, ok, fileBytes = 50_000, durationMs = 999))
            .isEqualTo(VideoEnd.DISCARD)
    }

    @Test
    fun `a recording of exactly one second is kept`() {
        assertThat(videoFinalizeOutcome(VideoEnd.SEND, ok, fileBytes = 50_000, durationMs = 1_000))
            .isEqualTo(VideoEnd.SEND)
    }

    @Test
    fun `the first end request wins`() {
        assertThat(nextVideoEnd(null, VideoEnd.SEND)).isEqualTo(VideoEnd.SEND)
        assertThat(nextVideoEnd(VideoEnd.SEND, VideoEnd.DRAFT)).isEqualTo(VideoEnd.SEND)
        assertThat(nextVideoEnd(VideoEnd.DRAFT, VideoEnd.SEND)).isEqualTo(VideoEnd.DRAFT)
    }

    @Test
    fun `a later cancel overrides a pending send or draft`() {
        assertThat(nextVideoEnd(VideoEnd.SEND, VideoEnd.DISCARD)).isEqualTo(VideoEnd.DISCARD)
        assertThat(nextVideoEnd(VideoEnd.DRAFT, VideoEnd.DISCARD)).isEqualTo(VideoEnd.DISCARD)
    }

    @Test
    fun `a send nobody is waiting for becomes a draft`() {
        assertThat(videoEndWithoutWaiter(VideoEnd.SEND)).isEqualTo(VideoEnd.DRAFT)
    }

    @Test
    fun `losing the waiter never revives a cancelled or drafted recording`() {
        assertThat(videoEndWithoutWaiter(VideoEnd.DISCARD)).isEqualTo(VideoEnd.DISCARD)
        assertThat(videoEndWithoutWaiter(VideoEnd.DRAFT)).isEqualTo(VideoEnd.DRAFT)
        assertThat(videoEndWithoutWaiter(null)).isEqualTo(null)
    }
}
