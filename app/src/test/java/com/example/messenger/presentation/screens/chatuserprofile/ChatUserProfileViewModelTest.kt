package com.example.messenger.presentation.screens.chatuserprofile

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.example.messenger.data.media.ClipPlaybackController
import com.example.messenger.data.media.ClipPlaybackState
import com.example.messenger.data.media.ClipQueueItem
import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.User
import com.example.messenger.domain.model.voicemessage.Clip
import com.example.messenger.domain.usecase.user.GetUserByIdUseCase
import com.example.messenger.testutil.MainDispatcherExtension
import com.example.messenger.testutil.fakes.FakeClipRepository
import com.example.messenger.testutil.fakes.FakeConversationRepository
import com.example.messenger.testutil.fakes.FakeMediaRepository
import com.example.messenger.testutil.fakes.FakeMessageRepository
import com.example.messenger.testutil.fakes.FakeUserRepository
import com.example.messenger.util.DELETED_USER_NAME
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ChatUserProfileViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val userRepository = FakeUserRepository()
    private val conversationRepository = FakeConversationRepository()
    private val messageRepository = FakeMessageRepository()
    private val mediaRepository = FakeMediaRepository()
    private val clipRepository = FakeClipRepository()
    private val playbackController = mockk<ClipPlaybackController>(relaxed = true) {
        every { state } returns MutableStateFlow(ClipPlaybackState())
    }

    private fun createViewModel(userId: String = PARTNER, conversationId: String? = CONVERSATION) =
        ChatUserProfileViewModel(
            getUserByIdUseCase = GetUserByIdUseCase(userRepository),
            userRepository = userRepository,
            conversationRepository = conversationRepository,
            messageRepository = messageRepository,
            mediaRepository = mediaRepository,
            clipRepository = clipRepository,
            playbackController = playbackController,
            savedStateHandle = SavedStateHandle(mapOf("userId" to userId, "conversationId" to conversationId)),
        )

    private fun photoMessage(id: String, timestamp: Long) = Message.Media(
        id = id,
        conversationId = CONVERSATION,
        timestamp = timestamp,
        items = listOf(MediaItem(id = "item-$id", kind = MediaItem.IMAGE)),
    )

    private fun voiceMessage(id: String, durationMs: Long = 4_000, timestamp: Long = 100) = Message.Clip(
        id = id,
        senderId = PARTNER,
        timestamp = timestamp,
        clip = Clip.Voice(durationMs = durationMs),
    )

    private fun fullPage(): List<Message> =
        List(ChatUserProfileViewModel.MEDIA_PAGE_SIZE.toInt()) { index -> photoMessage("r$index", 10_000L - index) }

    @Test
    fun `contact alias overrides the username`() = runTest {
        userRepository.usersById[PARTNER] = User(id = PARTNER, username = "Bob")
        userRepository.contactAliases.value = mapOf(PARTNER to "Bobby")

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.displayName).isEqualTo("Bobby")
    }

    @Test
    fun `an online user is shown online`() = runTest {
        userRepository.usersById[PARTNER] = User(id = PARTNER, username = "Bob", isOnline = true)

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.isOnline).isTrue()
    }

    @Test
    fun `a deleted user is never shown online`() = runTest {
        userRepository.usersById[PARTNER] = User(id = PARTNER, username = "Bob", isOnline = true, deleted = true)
        userRepository.contactAliases.value = mapOf(PARTNER to "Bobby")

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.isOnline).isFalse()
        assertThat(viewModel.uiState.value.displayName).isEqualTo(DELETED_USER_NAME)
    }

    @Test
    fun `media already on the phone shows at once`() = runTest {
        messageRepository.messages.value = listOf(photoMessage("m1", 100))

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.sharedMedia.photos.map { it.message.id }).containsExactly("m1")
        assertThat(messageRepository.sharedMediaRequests).containsExactly(CONVERSATION to null)
    }

    @Test
    fun `opened from contacts it finds the one-to-one chat with that person`() = runTest {
        conversationRepository.conversations.value = listOf(
            Conversation(id = "other", participantIds = listOf(ME, "someone-else")),
            Conversation(id = "theirs", participantIds = listOf(ME, PARTNER)),
        )

        createViewModel(conversationId = null)

        assertThat(messageRepository.sharedMediaRequests).containsExactly("theirs" to null)
    }

    @Test
    fun `a group chat that includes the person is not taken for the one-to-one chat`() = runTest {
        conversationRepository.conversations.value = listOf(
            Conversation(id = "group", participantIds = listOf(ME, PARTNER, "someone-else")),
            Conversation(id = "theirs", participantIds = listOf(ME, PARTNER)),
        )

        createViewModel(conversationId = null)

        assertThat(messageRepository.sharedMediaRequests).containsExactly("theirs" to null)
    }

    @Test
    fun `without a chat there is nothing to load`() = runTest {
        val viewModel = createViewModel(conversationId = null)

        val state = viewModel.uiState.value
        assertThat(state.mediaLoading).isFalse()
        assertThat(state.mediaEndReached).isTrue()
        assertThat(messageRepository.sharedMediaRequests).isEmpty()
    }

    @Test
    fun `server media is merged with phone media without duplicates`() = runTest {
        messageRepository.messages.value = listOf(photoMessage("m1", 100))
        messageRepository.sharedMediaPage = { Result.success(listOf(photoMessage("m1", 100), photoMessage("m0", 50))) }

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.sharedMedia.photos.map { it.message.id }).containsExactly("m1", "m0")
    }

    @Test
    fun `the phone copy of a message wins over its server copy`() = runTest {
        val onPhone = photoMessage("m1", 100).copy(caption = "on the phone")
        messageRepository.messages.value = listOf(onPhone)
        messageRepository.sharedMediaPage = { Result.success(listOf(photoMessage("m1", 100).copy(caption = "on the server"))) }

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.sharedMedia.photos.map { it.message }).containsExactly(onPhone)
    }

    @Test
    fun `a page request still in flight is not sent twice`() = runTest {
        val release = CompletableDeferred<Result<List<Message>>>()
        messageRepository.sharedMediaPage = { release.await() }
        val viewModel = createViewModel()

        viewModel.loadMoreMedia()

        assertThat(messageRepository.sharedMediaRequests).containsExactly(CONVERSATION to null)
        release.complete(Result.success(emptyList()))
    }

    @Test
    fun `each loaded page is counted so the screen can ask for the next one`() = runTest {
        val first = fullPage()
        messageRepository.sharedMediaPage = { before -> Result.success(if (before == null) first else emptyList()) }
        val viewModel = createViewModel()
        assertThat(viewModel.uiState.value.mediaPagesLoaded).isEqualTo(1)

        viewModel.loadMoreMedia()

        assertThat(viewModel.uiState.value.mediaPagesLoaded).isEqualTo(2)
    }

    @Test
    fun `loading more asks for media older than the last server page`() = runTest {
        val first = fullPage()
        messageRepository.sharedMediaPage = { before -> Result.success(if (before == null) first else emptyList()) }
        val viewModel = createViewModel()

        viewModel.loadMoreMedia()

        assertThat(messageRepository.sharedMediaRequests)
            .containsExactly(CONVERSATION to null, CONVERSATION to first.last().id)
        assertThat(viewModel.uiState.value.mediaEndReached).isTrue()
    }

    @Test
    fun `a short page ends paging`() = runTest {
        messageRepository.sharedMediaPage = { Result.success(listOf(photoMessage("m1", 100))) }
        val viewModel = createViewModel()

        viewModel.loadMoreMedia()

        assertThat(viewModel.uiState.value.mediaEndReached).isTrue()
        assertThat(messageRepository.sharedMediaRequests).containsExactly(CONVERSATION to null)
    }

    @Test
    fun `a failed server request stops paging and keeps phone media`() = runTest {
        messageRepository.messages.value = listOf(photoMessage("m1", 100))
        messageRepository.sharedMediaPage = { Result.failure(IllegalStateException("index missing")) }

        val viewModel = createViewModel()
        viewModel.loadMoreMedia()

        val state = viewModel.uiState.value
        assertThat(state.mediaLoading).isFalse()
        assertThat(state.mediaEndReached).isTrue()
        assertThat(state.sharedMedia.photos.map { it.message.id }).containsExactly("m1")
        assertThat(messageRepository.sharedMediaRequests).containsExactly(CONVERSATION to null)
    }

    @Test
    fun `playing a voice message hands it to the clip player`() = runTest {
        val voice = voiceMessage("v1", durationMs = 7_000)
        messageRepository.sharedMediaPage = { Result.success(listOf(voice)) }
        val viewModel = createViewModel()

        viewModel.toggleVoice("v1")

        assertThat(clipRepository.registeredRemote.map { it.id }).containsExactly("v1")
        verify { playbackController.toggle(ClipQueueItem("v1", CONVERSATION, 7_000)) }
    }

    @Test
    fun `tapping a voice message plays that message`() = runTest {
        messageRepository.messages.value = listOf(voiceMessage("newest", timestamp = 300))
        messageRepository.sharedMediaPage = { Result.success(listOf(voiceMessage("older", durationMs = 9_000, timestamp = 100))) }
        val viewModel = createViewModel()

        viewModel.toggleVoice("older")

        verify { playbackController.toggle(ClipQueueItem("older", CONVERSATION, 9_000)) }
    }

    @Test
    fun `playing a voice message lets the player continue through the rest of the list`() = runTest {
        messageRepository.messages.value = listOf(voiceMessage("local"))
        messageRepository.sharedMediaPage = { Result.success(listOf(voiceMessage("older"), voiceMessage("oldest"))) }
        val viewModel = createViewModel()

        viewModel.toggleVoice("older")

        assertThat(clipRepository.registeredRemote.map { it.id }.toSet())
            .isEqualTo(setOf("local", "older", "oldest"))
    }

    @Test
    fun `the partner is known before their profile loads`() = runTest {
        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.partnerId).isEqualTo(PARTNER)
    }

    @Test
    fun `cancelling an upload from the grid cancels it in the media repository`() = runTest {
        val viewModel = createViewModel()

        viewModel.cancelUpload("m1", "item-m1")

        assertThat(mediaRepository.cancelledUploads).containsExactly("m1" to "item-m1")
    }

    @Test
    fun `cancelling a download from the grid cancels it in the media repository`() = runTest {
        val viewModel = createViewModel()

        viewModel.cancelDownload("i1")

        assertThat(mediaRepository.cancelledDownloads).containsExactly("i1")
    }

    @Test
    fun `tapping a tile that is not downloaded starts the download`() = runTest {
        val item = MediaItem(id = "i1", kind = MediaItem.IMAGE)
        val viewModel = createViewModel()

        viewModel.downloadMedia(item)

        assertThat(mediaRepository.downloaded).containsExactly(item)
    }

    @Test
    fun `download progress from the media repository reaches the state`() = runTest {
        val viewModel = createViewModel()
        val progress = mapOf("i1" to com.example.messenger.domain.model.MediaTransfer.Downloading(0.5f))

        mediaRepository.transfers.value = progress

        assertThat(viewModel.uiState.value.transfers).isEqualTo(progress)
    }

    private companion object {
        const val ME = "me"
        const val PARTNER = "partner"
        const val CONVERSATION = "c1"
    }
}
