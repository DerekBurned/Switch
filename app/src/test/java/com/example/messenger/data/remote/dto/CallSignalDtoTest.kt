package com.example.messenger.data.remote.dto

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.example.messenger.domain.model.CallSignal
import com.example.messenger.domain.model.CallStatus
import org.junit.jupiter.api.Test

class CallSignalDtoTest {

    @Test
    fun `video flag round-trips through the dto`() {
        val signal = CallSignal(
            callId = "c1",
            callerId = "u1",
            calleeId = "u2",
            channelName = "chan",
            status = CallStatus.RINGING,
            timestamp = 42L,
            video = true,
        )
        val mapped = signal.toDto().toCallSignal()
        assertThat(mapped).isEqualTo(signal)
        assertThat(mapped.video).isTrue()
    }

    @Test
    fun `video defaults to false for signals without the field`() {
        val dto = CallSignalDto(
            callId = "c1",
            callerId = "u1",
            calleeId = "u2",
            channelName = "chan",
            status = "RINGING",
            timestamp = 42L,
        )
        assertThat(dto.toCallSignal().video).isFalse()
    }

    @Test
    fun `unknown status still falls back to ringing with video preserved`() {
        val dto = CallSignalDto(status = "GARBAGE", video = true)
        val signal = dto.toCallSignal()
        assertThat(signal.status).isEqualTo(CallStatus.RINGING)
        assertThat(signal.video).isTrue()
    }
}
