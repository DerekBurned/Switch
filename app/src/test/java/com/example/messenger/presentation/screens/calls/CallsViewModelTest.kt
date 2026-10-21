package com.example.messenger.presentation.screens.calls

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.example.messenger.data.repository.CallHistoryRepository
import com.example.messenger.domain.model.CallHistoryEntry
import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.model.User
import com.example.messenger.domain.model.call.CallType
import com.example.messenger.testutil.AndroidLogExtension
import com.example.messenger.testutil.MainDispatcherExtension
import com.example.messenger.testutil.fakes.FakeConversationRepository
import com.example.messenger.testutil.fakes.FakeMessageRepository
import com.example.messenger.testutil.fakes.FakeUserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class CallsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    @JvmField
    @RegisterExtension
    val log = AndroidLogExtension()

    private val history = MutableStateFlow<List<CallHistoryEntry>>(emptyList())
    private val callHistoryRepository = mockk<CallHistoryRepository> {
        every { getCallHistory("me") } returns history
    }
    private val userRepository = FakeUserRepository()
    private val conversationRepository = FakeConversationRepository()
    private val messageRepository = FakeMessageRepository()
    private val me = mockk<FirebaseUser> { every { uid } returns "me" }
    private val firebaseAuth = mockk<FirebaseAuth> { every { currentUser } returns me }

    private fun createViewModel() = CallsViewModel(
        callHistoryRepository,
        userRepository,
        conversationRepository,
        messageRepository,
        firebaseAuth,
    )

    private fun entry(id: String, partnerId: String = "bob", partnerName: String = "Bob") = CallHistoryEntry(
        id = id,
        conversationId = "c1",
        partnerId = partnerId,
        partnerName = partnerName,
        partnerAvatarUrl = null,
        timestamp = 1,
        callType = CallType.ENDED,
        video = false,
        durationSeconds = 10,
        outgoing = true,
    )

    @Test
    fun `entries show the contact alias over the stored name`() = runTest(mainDispatcher.dispatcher) {
        history.value = listOf(entry("call1"))
        userRepository.contactAliases.value = mapOf("bob" to "Bobby")
        val viewModel = createViewModel()

        backgroundScope.launch { viewModel.calls.collect {} }
        runCurrent()

        assertThat(viewModel.calls.value.map { it.partnerName }).containsExactly("Bobby")
    }

    @Test
    fun `missing avatars are fetched once per partner`() = runTest(mainDispatcher.dispatcher) {
        userRepository.usersById["bob"] = User(id = "bob", avatarUrl = "bob.png")
        history.value = listOf(entry("call1"), entry("call2"))
        val viewModel = createViewModel()

        backgroundScope.launch { viewModel.calls.collect {} }
        runCurrent()
        history.value = listOf(entry("call1"), entry("call2"), entry("call3"))
        runCurrent()

        assertThat(viewModel.calls.value.map { it.partnerAvatarUrl })
            .containsExactly("bob.png", "bob.png", "bob.png")
        assertThat(userRepository.requestedUserIds).containsExactly("bob")
    }

    @Test
    fun `opening calls backfills the 20 most recent conversations`() = runTest(mainDispatcher.dispatcher) {
        conversationRepository.conversations.value = (1..25).map { i ->
            Conversation(id = "c$i", lastMessageTimestamp = i.toLong())
        }

        createViewModel()
        runCurrent()

        val backfilledIds = messageRepository.backfilled.map { it.first }
        assertThat(backfilledIds).hasSize(20)
        assertThat(backfilledIds.first()).isEqualTo("c25")
        assertThat(backfilledIds.last()).isEqualTo("c6")
    }
}
