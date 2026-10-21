package com.example.messenger.presentation.screens.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.example.messenger.R
import com.example.messenger.data.media.ClipPlaybackController
import com.example.messenger.data.media.ClipPlaybackState
import com.example.messenger.data.media.ClipRecording
import com.example.messenger.data.media.VideoNoteRecorder
import com.example.messenger.data.media.VideoRecordState
import com.example.messenger.data.media.VoiceDraftStore
import com.example.messenger.data.media.VoiceRecordState
import com.example.messenger.data.media.VoiceRecorder
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.data.repository.RecentContactsRepository
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.User
import com.example.messenger.domain.usecase.auth.GetCurrentUserUseCase
import com.example.messenger.domain.usecase.conversation.MarkConversationAsReadUseCase
import com.example.messenger.domain.usecase.message.DeleteMessageUseCase
import com.example.messenger.domain.usecase.message.GetMessagesUseCase
import com.example.messenger.domain.usecase.message.LoadOlderMessagesUseCase
import com.example.messenger.domain.usecase.message.MarkMessageAsReadUseCase
import com.example.messenger.domain.usecase.message.SendMessageUseCase
import com.example.messenger.domain.usecase.message.SyncMessagesUseCase
import com.example.messenger.domain.usecase.presence.ObserveUserPresenceUseCase
import com.example.messenger.domain.usecase.typing.ObserveTypingUseCase
import com.example.messenger.presentation.base.UiText
import com.example.messenger.testutil.AndroidLogExtension
import com.example.messenger.testutil.MainDispatcherExtension
import com.example.messenger.testutil.fakes.FakeAuthRepository
import com.example.messenger.testutil.fakes.FakeClipRepository
import com.example.messenger.testutil.fakes.FakeConversationRepository
import com.example.messenger.testutil.fakes.FakeMediaRepository
import com.example.messenger.testutil.fakes.FakeMessageRepository
import com.example.messenger.testutil.fakes.FakePresenceService
import com.example.messenger.testutil.fakes.FakeTypingService
import com.example.messenger.testutil.fakes.FakeUserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File

class ChatViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    @JvmField
    @RegisterExtension
    val log = AndroidLogExtension()

    private val messageRepository = FakeMessageRepository()
    private val conversationRepository = FakeConversationRepository()
    private val authRepository = FakeAuthRepository().apply { loggedInUser = User(id = "me") }
    private val mediaRepository = FakeMediaRepository()
    private val clipRepository = FakeClipRepository()
    private val typingService = FakeTypingService()
    private val voiceRecorder = mockk<VoiceRecorder>(relaxed = true) {
        every { state } returns MutableStateFlow<VoiceRecordState>(VoiceRecordState.Idle)
    }
    private val videoNoteRecorder = mockk<VideoNoteRecorder>(relaxed = true) {
        every { state } returns MutableStateFlow<VideoRecordState>(VideoRecordState.Idle)
    }
    private val voiceDrafts = mockk<VoiceDraftStore>(relaxed = true) {
        every { observe(any()) } returns flowOf(null)
    }
    private val playbackController = mockk<ClipPlaybackController>(relaxed = true) {
        every { state } returns MutableStateFlow(ClipPlaybackState())
    }
    private val authService = mockk<FirebaseAuthService> { every { getCurrentUserId() } returns "me" }

    private fun createViewModel() = ChatViewModel(
        sendMessageUseCase = SendMessageUseCase(messageRepository, GetCurrentUserUseCase(authRepository)),
        getMessagesUseCase = GetMessagesUseCase(messageRepository),
        syncMessagesUseCase = SyncMessagesUseCase(messageRepository),
        loadOlderMessagesUseCase = LoadOlderMessagesUseCase(messageRepository),
        markMessageAsReadUseCase = MarkMessageAsReadUseCase(messageRepository),
        markConversationAsReadUseCase = MarkConversationAsReadUseCase(conversationRepository),
        deleteMessageUseCase = DeleteMessageUseCase(messageRepository, clipRepository),
        observeUserPresenceUseCase = ObserveUserPresenceUseCase(FakePresenceService()),
        observeTypingUseCase = ObserveTypingUseCase(typingService),
        typingService = typingService,
        mediaRepository = mediaRepository,
        clipRepository = clipRepository,
        voiceRecorder = voiceRecorder,
        videoNoteRecorder = videoNoteRecorder,
        voiceDrafts = voiceDrafts,
        playbackController = playbackController,
        userRepository = FakeUserRepository(),
        recentContactsRepository = mockk<RecentContactsRepository>(relaxed = true),
        firebaseAuthService = authService,
        savedStateHandle = SavedStateHandle(mapOf("conversationId" to "c1")),
    )

    private fun text(id: String, senderId: String = "bob", status: MessageStatus = MessageStatus.SENT) =
        Message.Text(id = id, conversationId = "c1", senderId = senderId, status = status, text = id)

    @Test
    fun `messages from the repository land in state and loading clears`() = runTest {
        messageRepository.messages.value = listOf(text("m1"))

        val viewModel = createViewModel()

        assertThat(viewModel.state.value.messages.map { it.id }).containsExactly("m1")
        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    @Test
    fun `first unread partner message becomes the unread anchor and stays fixed`() = runTest {
        messageRepository.messages.value = listOf(
            text("m1", status = MessageStatus.READ),
            text("m2"),
        )
        val viewModel = createViewModel()

        messageRepository.messages.value = listOf(
            text("m1", status = MessageStatus.READ),
            text("m2", status = MessageStatus.READ),
            text("m3"),
        )

        assertThat(viewModel.state.value.firstUnreadMessageId).isEqualTo("m2")
    }

    @Test
    fun `sending text sends the reply target and then clears it`() = runTest {
        val viewModel = createViewModel()
        viewModel.dispatch(ChatIntent.SetReplyTo(text("r1")))

        viewModel.dispatch(ChatIntent.SendMessage("hi"))

        val sent = messageRepository.sent.single() as Message.Text
        assertThat(sent.replyToMessageId).isEqualTo("r1")
        assertThat(viewModel.state.value.replyingTo).isNull()
        assertThat(viewModel.state.value.isSending).isFalse()
    }

    @Test
    fun `pending attachments are sent as one media message instead of text`() = runTest {
        val viewModel = createViewModel()
        viewModel.dispatch(ChatIntent.AddAttachment(mockk<Uri>(), "IMAGE"))
        viewModel.dispatch(ChatIntent.AddAttachment(mockk<Uri>(), "VIDEO"))

        viewModel.dispatch(ChatIntent.SendMessage("  look  "))

        val (conversationId, uris, caption) = mediaRepository.sentMedia.single()
        assertThat(conversationId).isEqualTo("c1")
        assertThat(uris).hasSize(2)
        assertThat(caption).isEqualTo("look")
        assertThat(messageRepository.sent).isEmpty()
        assertThat(viewModel.state.value.pendingAttachments).isEmpty()
    }

    @Test
    fun `adding the same attachment twice keeps one`() = runTest {
        val viewModel = createViewModel()
        val uri = mockk<Uri>()

        viewModel.dispatch(ChatIntent.AddAttachment(uri, "IMAGE"))
        viewModel.dispatch(ChatIntent.AddAttachment(uri, "IMAGE"))

        assertThat(viewModel.state.value.pendingAttachments).hasSize(1)
    }

    @Test
    fun `seen messages are marked read once each`() = runTest {
        val viewModel = createViewModel()
        val message = text("m1")

        viewModel.dispatch(ChatIntent.MessagesSeen(listOf(message)))
        viewModel.dispatch(ChatIntent.MessagesSeen(listOf(message)))

        assertThat(messageRepository.markedRead).containsExactly(message)
    }

    @Test
    fun `read receipts are skipped while the user id is blank`() = runTest {
        every { authService.getCurrentUserId() } returns null
        val viewModel = createViewModel()

        viewModel.dispatch(ChatIntent.MessagesSeen(listOf(text("m1"))))

        assertThat(messageRepository.markedRead).isEmpty()
    }

    @Test
    fun `jumping to a loaded message highlights it for 2500 ms`() = runTest(mainDispatcher.dispatcher) {
        messageRepository.messages.value = listOf(text("m1"))
        val viewModel = createViewModel()

        viewModel.dispatch(ChatIntent.JumpToMessage("m1"))
        assertThat(viewModel.state.value.highlightedMessageId).isEqualTo("m1")

        advanceTimeBy(2501)
        assertThat(viewModel.state.value.highlightedMessageId).isNull()
    }

    @Test
    fun `jumping to a message that never loads emits Original message not found`() =
        runTest(mainDispatcher.dispatcher) {
            messageRepository.messages.value = listOf(text("m1"))
            messageRepository.loadOlderResult = Result.success(0)
            val viewModel = createViewModel()

            viewModel.effect.test {
                viewModel.dispatch(ChatIntent.JumpToMessage("missing"))

                assertThat(awaitItem())
                    .isEqualTo(ChatEffect.ShowError(UiText.DynamicString("Original message not found")))
            }
        }

    @Test
    fun `older pages stop once a page returns fewer than 100`() = runTest {
        messageRepository.loadOlderResult = Result.success(40)
        val viewModel = createViewModel()

        viewModel.dispatch(ChatIntent.LoadOlderMessages)

        assertThat(viewModel.state.value.hasMoreOlder).isFalse()
        assertThat(viewModel.state.value.isLoadingOlder).isFalse()
    }

    @Test
    fun `a failed older page keeps paging enabled`() = runTest {
        messageRepository.loadOlderResult = Result.failure(Exception("offline"))
        val viewModel = createViewModel()

        viewModel.dispatch(ChatIntent.LoadOlderMessages)

        assertThat(viewModel.state.value.hasMoreOlder).isTrue()
    }

    @Test
    fun `typing sets typing and clears it after 2 s of silence`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = createViewModel()

        viewModel.dispatch(ChatIntent.TextChanged("h"))
        assertThat(typingService.setCount).isEqualTo(1)
        assertThat(typingService.clearCount).isEqualTo(0)

        advanceTimeBy(2001)
        assertThat(typingService.clearCount).isEqualTo(1)
    }

    @Test
    fun `delete failure emits ShowError`() = runTest {
        messageRepository.deleteResult = Result.failure(Exception("nope"))
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.dispatch(ChatIntent.DeleteMessage(text("m1"), forEveryone = false))

            assertThat(awaitItem()).isEqualTo(ChatEffect.ShowError(UiText.DynamicString("nope")))
        }
    }

    @Test
    fun `permanent mic denial shows the prompt`() = runTest {
        val viewModel = createViewModel()

        viewModel.dispatch(ChatIntent.MicPermissionDenied(permanently = true, video = true))

        assertThat(viewModel.state.value.micDenied).isTrue()
        assertThat(viewModel.state.value.micDeniedForVideo).isTrue()
    }

    @Test
    fun `temporary mic denial emits the voice permission error`() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.dispatch(ChatIntent.MicPermissionDenied(permanently = false))

            val effect = awaitItem() as ChatEffect.ShowError
            assertThat((effect.message as UiText.StringResource).id).isEqualTo(R.string.voice_permission_denied)
        }
        assertThat(viewModel.state.value.micDenied).isFalse()
    }

    @Test
    fun `starting a recording with no draft starts the voice recorder`() = runTest {
        every { voiceRecorder.start("c1") } returns true
        val viewModel = createViewModel()

        viewModel.dispatch(ChatIntent.StartRecording)

        verify { voiceRecorder.start("c1") }
    }

    @Test
    fun `starting a recording while a draft exists does not start the recorder`() = runTest {
        every { voiceDrafts.observe(any()) } returns flowOf(ClipRecording("c1", File("draft.m4a"), 1_000))
        val viewModel = createViewModel()

        viewModel.dispatch(ChatIntent.StartRecording)

        verify(exactly = 0) { voiceRecorder.start(any()) }
    }

    @Test
    fun `a recorder that cannot start emits the unavailable error`() = runTest {
        every { voiceRecorder.start("c1") } returns false
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.dispatch(ChatIntent.StartRecording)

            val effect = awaitItem() as ChatEffect.ShowError
            assertThat((effect.message as UiText.StringResource).id).isEqualTo(R.string.voice_record_unavailable)
        }
    }
}
