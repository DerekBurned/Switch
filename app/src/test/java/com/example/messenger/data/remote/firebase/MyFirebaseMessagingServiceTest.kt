package com.example.messenger.data.remote.firebase

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.messenger.data.remote.call.ActiveCallHolder
import com.example.messenger.data.remote.call.CallForegroundService
import com.example.messenger.data.remote.call.IncomingCallGate
import com.example.messenger.data.remote.call.telecom.TelecomCallManager
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.model.call.CallStatus
import com.example.messenger.domain.service.ICallSignalingService
import com.example.messenger.testutil.AndroidLogExtension
import com.example.messenger.testutil.fakes.FakeFeatureFlags
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.EntryPointAccessors
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class MyFirebaseMessagingServiceTest {

    @JvmField
    @RegisterExtension
    val log = AndroidLogExtension()

    private val featureFlags = FakeFeatureFlags()
    private val signaling = mockk<ICallSignalingService>(relaxed = true)
    private val gate = IncomingCallGate(featureFlags, signaling)
    private val telecomCallManager = mockk<TelecomCallManager>(relaxed = true)
    private val entry = mockk<FcmEntryPoint>()
    private val context = mockk<Context>(relaxed = true)
    private val startIntent = mockk<Intent>()
    private val service = spyk(MyFirebaseMessagingService())

    private val callPush = mockk<RemoteMessage> {
        every { from } returns "fcm"
        every { data } returns mapOf(
            "type" to "call",
            "callId" to "c1",
            "callerId" to "alice",
            "callerName" to "Alice",
            "channelName" to "ch1",
            "video" to "true",
        )
    }

    @BeforeEach
    fun setUp() {
        ActiveCallHolder.clear()
        every { entry.incomingCallGate() } returns gate
        every { entry.telecomCallManager() } returns telecomCallManager
        every { service.applicationContext } returns context
        mockkStatic(EntryPointAccessors::class)
        every { EntryPointAccessors.fromApplication(any<Context>(), FcmEntryPoint::class.java) } returns entry
        val user = mockk<FirebaseUser> { every { uid } returns "bob" }
        val auth = mockk<FirebaseAuth> { every { currentUser } returns user }
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns auth
        mockkStatic(ContextCompat::class)
        every { ContextCompat.startForegroundService(any(), any()) } just Runs
        mockkObject(CallForegroundService.Companion)
        every {
            CallForegroundService.incomingIntent(any(), any(), any(), any(), any(), any(), any(), any())
        } returns startIntent
    }

    @AfterEach
    fun tearDown() {
        ActiveCallHolder.clear()
        unmockkStatic(EntryPointAccessors::class)
        unmockkStatic(FirebaseAuth::class)
        unmockkStatic(ContextCompat::class)
        unmockkObject(CallForegroundService.Companion)
    }

    @Test
    fun `a pushed call is declined and never started while calls are switched off`() {
        featureFlags.setOff(FeatureFlag.CALLS)

        service.onMessageReceived(callPush)

        coVerify { signaling.updateCallStatus("bob", "c1", CallStatus.DECLINED) }
        verify(exactly = 0) { ContextCompat.startForegroundService(any(), any()) }
        verify(exactly = 0) { telecomCallManager.addIncoming(any()) }
    }

    @Test
    fun `a pushed call starts the call service and the telecom call while calls are on`() {
        service.onMessageReceived(callPush)

        verify { ContextCompat.startForegroundService(any(), startIntent) }
        verify {
            telecomCallManager.addIncoming(match { it.callId == "c1" && it.partnerName == "Alice" && it.isVideo })
        }
    }

    @Test
    fun `a call the foreground listener already started is not started again`() {
        runBlocking { gate.admit("bob", "c1") }

        service.onMessageReceived(callPush)

        verify(exactly = 0) { ContextCompat.startForegroundService(any(), any()) }
        verify(exactly = 0) { telecomCallManager.addIncoming(any()) }
    }
}
