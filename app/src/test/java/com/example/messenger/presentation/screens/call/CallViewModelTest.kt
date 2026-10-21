package com.example.messenger.presentation.screens.call

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.example.messenger.data.remote.call.ActiveCallHolder
import com.example.messenger.data.remote.call.telecom.TelecomCallManager
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.service.ICallService
import com.example.messenger.testutil.AndroidLogExtension
import com.example.messenger.testutil.MainDispatcherExtension
import com.example.messenger.testutil.fakes.FakeConversationRepository
import com.example.messenger.testutil.fakes.FakeFeatureFlags
import com.example.messenger.testutil.fakes.FakeUserRepository
import com.google.firebase.auth.FirebaseAuth
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class CallViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    @JvmField
    @RegisterExtension
    val log = AndroidLogExtension()

    private val userRepository = FakeUserRepository()
    private val conversationRepository = FakeConversationRepository()
    private val featureFlags = FakeFeatureFlags()
    private val telecomCallManager = mockk<TelecomCallManager>(relaxed = true)

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = CallViewModel(
        application = mockk<Application>(relaxed = true),
        auth = mockk<FirebaseAuth>(),
        conversationRepository = conversationRepository,
        userRepository = userRepository,
        telecomCallManager = telecomCallManager,
        callService = mockk<ICallService>(relaxed = true),
        featureFlags = featureFlags,
        savedStateHandle = savedStateHandle,
    )

    private fun outgoingArgs(video: Boolean = false) = SavedStateHandle(
        mapOf("partnerId" to "bob", "partnerName" to "Bob", "video" to video),
    )

    private fun activeCall(
        isVideoCall: Boolean = false,
        localVideoOn: Boolean = false,
        remoteVideoOn: Boolean = false,
        frontCamera: Boolean = true,
    ) = ActiveCallHolder.ActiveCall(
        callId = "c1",
        callerId = "alice",
        calleeId = "bob",
        channelName = "ch1",
        partnerName = "Bob",
        partnerPhone = "",
        isIncoming = false,
        isActive = true,
        isVideoCall = isVideoCall,
        localVideoOn = localVideoOn,
        remoteVideoOn = remoteVideoOn,
        frontCamera = frontCamera,
    )

    @BeforeEach
    fun setUp() {
        ActiveCallHolder.clear()
    }

    @AfterEach
    fun tearDown() {
        ActiveCallHolder.clear()
    }

    @Test
    fun `video call fields are mirrored into ui state`() = runTest {
        val viewModel = createViewModel()

        ActiveCallHolder.set(
            activeCall(isVideoCall = true, localVideoOn = true, remoteVideoOn = false, frontCamera = false),
        )

        val state = viewModel.uiState.value
        assertThat(state.isVideoCall).isTrue()
        assertThat(state.localVideoOn).isTrue()
        assertThat(state.remoteVideoOn).isFalse()
        assertThat(state.frontCamera).isFalse()
    }

    @Test
    fun `remote camera turning on is mirrored independently of local camera`() = runTest {
        val viewModel = createViewModel()

        ActiveCallHolder.set(activeCall(isVideoCall = true, localVideoOn = false, remoteVideoOn = true))

        val state = viewModel.uiState.value
        assertThat(state.isVideoCall).isTrue()
        assertThat(state.localVideoOn).isFalse()
        assertThat(state.remoteVideoOn).isTrue()
    }

    @Test
    fun `audio only call keeps video fields at their defaults`() = runTest {
        val viewModel = createViewModel()

        ActiveCallHolder.set(activeCall())

        val state = viewModel.uiState.value
        assertThat(state.isVideoCall).isFalse()
        assertThat(state.localVideoOn).isFalse()
        assertThat(state.remoteVideoOn).isFalse()
        assertThat(state.frontCamera).isTrue()
    }

    @Test
    fun `outgoing call is allowed when calls are switched on`() = runTest {
        val viewModel = createViewModel(outgoingArgs(video = true))

        assertThat(viewModel.outgoingAllowed).isTrue()
    }

    @Test
    fun `outgoing call reports unavailable and starts nothing when calls are switched off`() = runTest {
        featureFlags.setOff(FeatureFlag.CALLS)
        val viewModel = createViewModel(outgoingArgs())

        viewModel.callExit.test {
            viewModel.startOutgoing()
            assertThat(awaitItem()).isEqualTo(CallExit.Unavailable)
        }
        assertThat(viewModel.outgoingAllowed).isFalse()
        verify(exactly = 0) { telecomCallManager.placeOutgoing(any()) }
    }

    @Test
    fun `video call is blocked when only video calls are switched off`() = runTest {
        featureFlags.setOff(FeatureFlag.CALLS_VIDEO)

        assertThat(createViewModel(outgoingArgs(video = true)).outgoingAllowed).isFalse()
        assertThat(createViewModel(outgoingArgs(video = false)).outgoingAllowed).isTrue()
    }
}
