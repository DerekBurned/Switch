package com.example.messenger.data.remote.call

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import assertk.assertThat
import assertk.assertions.isTrue
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.data.remote.call.telecom.TelecomCallManager
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.model.call.CallSignal
import com.example.messenger.domain.model.call.CallStatus
import com.example.messenger.domain.repository.IUserRepository
import com.example.messenger.domain.service.ICallSignalingService
import com.example.messenger.testutil.AndroidLogExtension
import com.example.messenger.testutil.fakes.FakeFeatureFlags
import com.google.firebase.auth.FirebaseAuth
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class IncomingCallCoordinatorTest {

    @JvmField
    @RegisterExtension
    val log = AndroidLogExtension()

    private val signaling = mockk<ICallSignalingService>(relaxed = true)
    private val authService = mockk<FirebaseAuthService>()
    private val userRepository = mockk<IUserRepository>(relaxed = true)
    private val telecomCallManager = mockk<TelecomCallManager>(relaxed = true)
    private val featureFlags = FakeFeatureFlags()
    private val gate = IncomingCallGate(featureFlags, signaling)
    private val startIntent = mockk<Intent>()

    private val signal = CallSignal(
        callId = "c1",
        callerId = "alice",
        calleeId = "bob",
        channelName = "ch1",
        video = false,
    )

    @BeforeEach
    fun setUp() {
        ActiveCallHolder.clear()
        mockkStatic(ContextCompat::class)
        every { ContextCompat.startForegroundService(any(), any()) } just Runs
        mockkObject(CallForegroundService.Companion)
        every {
            CallForegroundService.incomingIntent(any(), any(), any(), any(), any(), any(), any(), any())
        } returns startIntent
        every { authService.getCurrentUserId() } returns "bob"
        every { signaling.observeIncomingCall("bob") } returns flowOf(signal)
        coEvery { userRepository.getUserById("alice") } returns Result.success(null)
    }

    @AfterEach
    fun tearDown() {
        ActiveCallHolder.clear()
        unmockkStatic(ContextCompat::class)
        unmockkObject(CallForegroundService.Companion)
    }

    private fun createCoordinator() = IncomingCallCoordinator(
        context = mockk<Context>(relaxed = true),
        signalingService = signaling,
        firebaseAuthService = authService,
        firebaseAuth = mockk<FirebaseAuth>(relaxed = true),
        userRepository = userRepository,
        telecomCallManager = telecomCallManager,
        incomingCallGate = gate,
    )

    @Test
    fun `an incoming call is declined and never started while calls are switched off`() {
        featureFlags.setOff(FeatureFlag.CALLS)

        createCoordinator().start()

        coVerify(timeout = 5_000) { signaling.updateCallStatus("bob", "c1", CallStatus.DECLINED) }
        verify(exactly = 0) { ContextCompat.startForegroundService(any(), any()) }
        verify(exactly = 0) { telecomCallManager.addIncoming(any()) }
    }

    @Test
    fun `an incoming call starts the call service and the telecom call while calls are on`() {
        createCoordinator().start()

        verify(timeout = 5_000) { ContextCompat.startForegroundService(any(), startIntent) }
        verify(timeout = 5_000) {
            telecomCallManager.addIncoming(match { it.callId == "c1" && it.isIncoming && !it.isVideo })
        }
    }

    @Test
    fun `a call whose caller lookup is interrupted stays free for the push path`() {
        val signals = Channel<CallSignal?>(Channel.UNLIMITED)
        every { signaling.observeIncomingCall("bob") } returns signals.receiveAsFlow()
        coEvery { userRepository.getUserById("alice") } coAnswers { awaitCancellation() }
        signals.trySend(signal)

        createCoordinator().start()
        coVerify(timeout = 5_000) { userRepository.getUserById("alice") }
        signals.trySend(null)

        assertThat(runBlocking { gate.admit("bob", "c1") }).isTrue()
        verify(exactly = 0) { ContextCompat.startForegroundService(any(), any()) }
    }
}
