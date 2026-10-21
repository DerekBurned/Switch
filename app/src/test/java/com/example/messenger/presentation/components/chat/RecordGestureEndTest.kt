package com.example.messenger.presentation.components.chat

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class RecordGestureEndTest {

    @Test
    fun `releasing sends`() {
        assertThat(recordGestureEnd(locked = false, slidToCancel = false, released = true))
            .isEqualTo(RecordGestureEnd.SEND)
    }

    @Test
    fun `a system cancel keeps a draft`() {
        assertThat(recordGestureEnd(locked = false, slidToCancel = false, released = false))
            .isEqualTo(RecordGestureEnd.DRAFT)
    }

    @Test
    fun `a locked recording keeps going`() {
        assertThat(recordGestureEnd(locked = true, slidToCancel = false, released = true))
            .isEqualTo(RecordGestureEnd.NONE)
        assertThat(recordGestureEnd(locked = true, slidToCancel = false, released = false))
            .isEqualTo(RecordGestureEnd.NONE)
    }

    @Test
    fun `sliding to cancel does nothing more`() {
        assertThat(recordGestureEnd(locked = false, slidToCancel = true, released = false))
            .isEqualTo(RecordGestureEnd.NONE)
    }

    @Test
    fun `drag progress grows with the distance and clamps at one`() {
        assertThat(dragProgress(0f, 80f)).isEqualTo(0f)
        assertThat(dragProgress(40f, 80f)).isEqualTo(0.5f)
        assertThat(dragProgress(200f, 80f)).isEqualTo(1f)
    }

    @Test
    fun `dragging the wrong way gives no progress`() {
        assertThat(dragProgress(-30f, 80f)).isEqualTo(0f)
    }

    @Test
    fun `a zero threshold never divides by zero`() {
        assertThat(dragProgress(10f, 0f)).isEqualTo(0f)
    }
}
