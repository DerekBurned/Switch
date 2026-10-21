package com.example.messenger.data.media

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class WaveformTest {

    @Test
    fun `downsampling always yields the requested bucket count`() {
        val samples = List(1000) { it % 100 }
        assertThat(downsampleWaveform(samples, buckets = 50).size).isEqualTo(50)
    }

    @Test
    fun `an empty recording yields a flat waveform`() {
        assertThat(downsampleWaveform(emptyList(), buckets = 8)).isEqualTo(List(8) { 0 })
    }

    @Test
    fun `a silent recording yields a flat waveform`() {
        assertThat(downsampleWaveform(List(200) { 0 }, buckets = 10)).isEqualTo(List(10) { 0 })
    }

    @Test
    fun `a recording shorter than the bucket count is stretched not padded`() {
        val result = downsampleWaveform(listOf(10, 20), buckets = 6)
        assertThat(result.size).isEqualTo(6)
        assertThat(result.all { it > 0 }).isTrue()
    }

    @Test
    fun `buckets average the samples they cover`() {
        val result = downsampleWaveform(listOf(0, 10, 100, 110), buckets = 2)
        assertThat(result).isEqualTo(listOf(5, 105))
    }

    @Test
    fun `a fractional bucket average is truncated`() {
        assertThat(downsampleWaveform(listOf(1, 2, 2, 2), buckets = 1)).isEqualTo(listOf(1))
    }

    @Test
    fun `a constant recording keeps its level across every bucket`() {
        assertThat(downsampleWaveform(List(500) { 42 }, buckets = 20)).isEqualTo(List(20) { 42 })
    }

    @Test
    fun `zero buckets yields nothing rather than throwing`() {
        assertThat(downsampleWaveform(List(10) { 5 }, buckets = 0)).isEqualTo(emptyList<Int>())
    }

    @Test
    fun `a single sample fills every bucket`() {
        assertThat(downsampleWaveform(listOf(77), buckets = 4)).isEqualTo(List(4) { 77 })
    }

    @Test
    fun `silence maps to zero and full scale to a hundred`() {
        assertThat(amplitudeToLevel(0)).isEqualTo(0)
        assertThat(amplitudeToLevel(32_767)).isEqualTo(100)
    }

    @Test
    fun `every ten decibels is a fifth of the bar`() {
        assertThat(amplitudeToLevel(1_843)).isEqualTo(50)
        assertThat(amplitudeToLevel(328)).isEqualTo(20)
    }

    @Test
    fun `levels round to the nearest step`() {
        assertThat(amplitudeToLevel(1_908)).isEqualTo(51)
    }

    @Test
    fun `anything below the silence floor stays at zero`() {
        assertThat(amplitudeToLevel(1)).isEqualTo(0)
        assertThat(amplitudeToLevel(100)).isEqualTo(0)
    }

    @Test
    fun `speech on a quiet microphone still reaches a visible level`() {
        assertThat(amplitudeToLevel(1_000)).isGreaterThan(30)
    }

    @Test
    fun `levels rise with amplitude`() {
        val levels = listOf(50, 500, 2_000, 8_000, 32_000).map(::amplitudeToLevel)
        assertThat(levels).isEqualTo(levels.sorted())
        assertThat(levels.toSet().size).isEqualTo(levels.size)
    }

    @Test
    fun `a quiet legacy waveform is stretched so its peak fills the bar`() {
        assertThat(normalizeWaveform(listOf(0, 1, 3, 6, 2))).isEqualTo(listOf(0, 17, 50, 100, 33))
    }

    @Test
    fun `normalising a flat or empty waveform changes nothing`() {
        assertThat(normalizeWaveform(List(5) { 0 })).isEqualTo(List(5) { 0 })
        assertThat(normalizeWaveform(emptyList())).isEqualTo(emptyList<Int>())
    }

    @Test
    fun `normalising leaves a waveform that already peaks at a hundred alone`() {
        val full = listOf(10, 100, 40)
        assertThat(normalizeWaveform(full)).isEqualTo(full)
    }

    @Test
    fun `out of range stored levels are clamped`() {
        assertThat(normalizeWaveform(listOf(-5, 150, 50))).isEqualTo(listOf(0, 100, 50))
    }

    @Test
    fun `a negative stored level stays at zero when a quiet waveform is stretched`() {
        assertThat(normalizeWaveform(listOf(-5, 25, 50))).isEqualTo(listOf(0, 50, 100))
    }
}
