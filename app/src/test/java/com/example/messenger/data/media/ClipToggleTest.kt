package com.example.messenger.data.media

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class ClipToggleTest {

    @Test
    fun `tapping another clip plays it`() {
        val playingA = ClipPlaybackState(messageId = "a", isPlaying = true)
        assertThat(clipToggle(playingA, loadedMessageId = "a", target = "b")).isEqualTo(ClipToggle.PLAY)
        assertThat(clipToggle(ClipPlaybackState(), loadedMessageId = null, target = "b")).isEqualTo(ClipToggle.PLAY)
    }

    @Test
    fun `tapping the playing clip pauses it`() {
        val playing = ClipPlaybackState(messageId = "a", isPlaying = true)
        assertThat(clipToggle(playing, loadedMessageId = "a", target = "a")).isEqualTo(ClipToggle.PAUSE)
    }

    @Test
    fun `tapping the paused loaded clip resumes it`() {
        val paused = ClipPlaybackState(messageId = "a", isPlaying = false)
        assertThat(clipToggle(paused, loadedMessageId = "a", target = "a")).isEqualTo(ClipToggle.RESUME)
    }

    @Test
    fun `tapping a clip that is still loading is ignored`() {
        val loading = ClipPlaybackState(messageId = "a", isLoading = true)
        assertThat(clipToggle(loading, loadedMessageId = null, target = "a")).isEqualTo(ClipToggle.IGNORE)
    }

    @Test
    fun `tapping a clip whose playback failed plays it again`() {
        val failed = ClipPlaybackState(failedMessageId = "a")
        assertThat(clipToggle(failed, loadedMessageId = null, target = "a")).isEqualTo(ClipToggle.PLAY)
    }

    @Test
    fun `tapping the target while the player still holds another clip plays the target`() {
        val stale = ClipPlaybackState(messageId = "b")
        assertThat(clipToggle(stale, loadedMessageId = "a", target = "b")).isEqualTo(ClipToggle.PLAY)
    }
}
