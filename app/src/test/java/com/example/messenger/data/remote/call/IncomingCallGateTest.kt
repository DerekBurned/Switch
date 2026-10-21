package com.example.messenger.data.remote.call

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.model.call.CallStatus
import com.example.messenger.domain.service.ICallSignalingService
import com.example.messenger.testutil.AndroidLogExtension
import com.example.messenger.testutil.fakes.FakeFeatureFlags
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class IncomingCallGateTest {

    @JvmField
    @RegisterExtension
    val log = AndroidLogExtension()

    private val featureFlags = FakeFeatureFlags()
    private val signaling = mockk<ICallSignalingService>(relaxed = true)
    private val gate = IncomingCallGate(featureFlags, signaling)

    @Test
    fun `admits a call when calls are switched on`() = runTest {
        assertThat(gate.admit("bob", "c1")).isTrue()

        coVerify(exactly = 0) { signaling.updateCallStatus(any(), any(), any()) }
    }

    @Test
    fun `declines a call when calls are switched off`() = runTest {
        featureFlags.setOff(FeatureFlag.CALLS)

        assertThat(gate.admit("bob", "c1")).isFalse()

        coVerify(exactly = 1) { signaling.updateCallStatus("bob", "c1", CallStatus.DECLINED) }
    }

    @Test
    fun `calls still ring when only video calls are switched off`() = runTest {
        featureFlags.setOff(FeatureFlag.CALLS_VIDEO)

        assertThat(gate.admit("bob", "c1")).isTrue()

        coVerify(exactly = 0) { signaling.updateCallStatus(any(), any(), any()) }
    }

    @Test
    fun `refreshes flags before deciding`() = runTest {
        gate.admit("bob", "c1")

        assertThat(featureFlags.awaitFreshCalls).isEqualTo(1)
    }

    @Test
    fun `a failed decline still keeps the call out`() = runTest {
        featureFlags.setOff(FeatureFlag.CALLS)
        coEvery { signaling.updateCallStatus(any(), any(), any()) } throws IllegalStateException("offline")

        assertThat(gate.admit("bob", "c1")).isFalse()
    }

    @Test
    fun `a call is admitted only once across incoming paths`() = runTest {
        assertThat(gate.admit("bob", "c1")).isTrue()
        assertThat(gate.admit("bob", "c1")).isFalse()
        assertThat(gate.admit("bob", "c2")).isTrue()
    }

    @Test
    fun `a decline that never completes gives up after the timeout`() = runTest {
        featureFlags.setOff(FeatureFlag.CALLS)
        coEvery { signaling.updateCallStatus(any(), any(), any()) } coAnswers { awaitCancellation() }

        assertThat(gate.admit("bob", "c1")).isFalse()
        assertThat(currentTime).isEqualTo(3_000L)
    }

    @Test
    fun `cancelling while the decline is pending stops the caller`() = runTest {
        featureFlags.setOff(FeatureFlag.CALLS)
        coEvery { signaling.updateCallStatus(any(), any(), any()) } coAnswers { awaitCancellation() }
        var continuedAfterAdmit = false

        val job = launch {
            gate.admit("bob", "c1")
            continuedAfterAdmit = true
        }
        runCurrent()
        job.cancel()
        job.join()

        assertThat(continuedAfterAdmit).isFalse()
    }
}
