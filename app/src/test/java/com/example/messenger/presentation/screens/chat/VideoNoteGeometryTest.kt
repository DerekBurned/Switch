package com.example.messenger.presentation.screens.chat

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class VideoNoteGeometryTest {

    @Test
    fun `ring fraction starts at twelve and runs clockwise`() {
        assertThat(ringFraction(50f, 0f, 50f, 50f)).isCloseTo(0f, 0.001f)
        assertThat(ringFraction(100f, 50f, 50f, 50f)).isCloseTo(0.25f, 0.001f)
        assertThat(ringFraction(50f, 100f, 50f, 50f)).isCloseTo(0.5f, 0.001f)
        assertThat(ringFraction(0f, 50f, 50f, 50f)).isCloseTo(0.75f, 0.001f)
    }

    @Test
    fun `just left of twelve is almost the whole circle`() {
        assertThat(ringFraction(49f, 0f, 50f, 50f)).isCloseTo(0.997f, 0.002f)
    }

    @Test
    fun `a touch near the edge is on the ring and the centre is not`() {
        assertThat(isOnRing(98f, 50f, 50f, 50f, radius = 50f, bandPx = 16f)).isTrue()
        assertThat(isOnRing(50f, 50f, 50f, 50f, radius = 50f, bandPx = 16f)).isFalse()
    }

    @Test
    fun `a touch just outside the circle still counts as the ring`() {
        assertThat(isOnRing(110f, 50f, 50f, 50f, radius = 50f, bandPx = 16f)).isTrue()
    }

    @Test
    fun `both edges of the ring band count as the ring`() {
        assertThat(isOnRing(116f, 50f, 50f, 50f, radius = 50f, bandPx = 16f)).isTrue()
        assertThat(isOnRing(84f, 50f, 50f, 50f, radius = 50f, bandPx = 16f)).isTrue()
        assertThat(isOnRing(117f, 50f, 50f, 50f, radius = 50f, bandPx = 16f)).isFalse()
    }

    @Test
    fun `the ring is measured by true distance from the centre`() {
        assertThat(isOnRing(50f, 2f, 50f, 50f, radius = 50f, bandPx = 16f)).isTrue()
        assertThat(isOnRing(100f, 100f, 50f, 50f, radius = 50f, bandPx = 16f)).isFalse()
    }

    @Test
    fun `a landscape video in a square view is widened, not squashed`() {
        assertThat(centerCropScale(640, 480, 300, 300)).isEqualTo(640f / 480f to 1f)
    }

    @Test
    fun `a portrait video in a square view is heightened`() {
        assertThat(centerCropScale(480, 640, 300, 300)).isEqualTo(1f to 640f / 480f)
    }

    @Test
    fun `an unknown video size leaves the view untouched`() {
        assertThat(centerCropScale(0, 0, 300, 300)).isEqualTo(1f to 1f)
        assertThat(centerCropScale(640, 480, 0, 0)).isEqualTo(1f to 1f)
    }

    @Test
    fun `a zero width or height on either side leaves the view untouched`() {
        assertThat(centerCropScale(640, 0, 300, 300)).isEqualTo(1f to 1f)
        assertThat(centerCropScale(640, 480, 300, 0)).isEqualTo(1f to 1f)
        assertThat(centerCropScale(0, 480, 300, 300)).isEqualTo(1f to 1f)
        assertThat(centerCropScale(640, 480, 0, 300)).isEqualTo(1f to 1f)
    }
}
