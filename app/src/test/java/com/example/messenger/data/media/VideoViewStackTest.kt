package com.example.messenger.data.media

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class VideoViewStackTest {

    @Test
    fun `the most recently attached view shows the video`() {
        val stack = VideoViewStack<String>()
        stack.attach("list")
        stack.attach("menu")
        assertThat(stack.current).isEqualTo("menu")
    }

    @Test
    fun `detaching the shown view hands the video back to the one underneath`() {
        val stack = VideoViewStack<String>()
        stack.attach("list")
        stack.attach("menu")
        stack.detach("menu")
        assertThat(stack.current).isEqualTo("list")
    }

    @Test
    fun `detaching a view that is not shown leaves the shown one alone`() {
        val stack = VideoViewStack<String>()
        stack.attach("old")
        stack.attach("new")
        stack.detach("old")
        assertThat(stack.current).isEqualTo("new")
    }

    @Test
    fun `detaching an unknown view changes nothing`() {
        val stack = VideoViewStack<String>()
        stack.attach("list")
        stack.detach("stranger")
        assertThat(stack.current).isEqualTo("list")
    }

    @Test
    fun `re-attaching a view brings it back to the top without duplicating it`() {
        val stack = VideoViewStack<String>()
        stack.attach("a")
        stack.attach("b")
        stack.attach("a")
        stack.detach("a")
        assertThat(stack.current).isEqualTo("b")
        stack.detach("b")
        assertThat(stack.current).isNull()
    }

    @Test
    fun `a finished voice clip advances to the next one`() {
        assertThat(shouldAutoAdvance(ClipPlaybackState(messageId = "v", isVideo = false))).isTrue()
    }

    @Test
    fun `a finished video note does not advance`() {
        assertThat(shouldAutoAdvance(ClipPlaybackState(messageId = "n", isVideo = true))).isFalse()
    }
}
