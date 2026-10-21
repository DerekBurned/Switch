package com.example.messenger.presentation.screens.chats

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.model.DomainUser
import com.example.messenger.domain.model.User
import com.example.messenger.domain.usecase.auth.GetCurrentUserUseCase
import com.example.messenger.domain.usecase.auth.ObserveAuthStateUseCase
import com.example.messenger.domain.usecase.conversation.ClearConversationForMeUseCase
import com.example.messenger.domain.usecase.conversation.CreateConversationUseCase
import com.example.messenger.domain.usecase.conversation.DeleteConversationForEveryoneUseCase
import com.example.messenger.domain.usecase.conversation.DeleteConversationUseCase
import com.example.messenger.domain.usecase.conversation.GetConversationsUseCase
import com.example.messenger.domain.usecase.conversation.MarkConversationAsReadUseCase
import com.example.messenger.domain.usecase.conversation.MarkConversationAsUnreadUseCase
import com.example.messenger.domain.usecase.conversation.SyncConversationsUseCase
import com.example.messenger.domain.usecase.presence.ObserveUserPresenceUseCase
import com.example.messenger.presentation.base.UiText
import com.example.messenger.testutil.AndroidLogExtension
import com.example.messenger.testutil.MainDispatcherExtension
import com.example.messenger.testutil.fakes.FakeAuthRepository
import com.example.messenger.testutil.fakes.FakeConversationRepository
import com.example.messenger.testutil.fakes.FakePresenceService
import com.example.messenger.testutil.fakes.FakeUserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ChatsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    @JvmField
    @RegisterExtension
    val log = AndroidLogExtension()

    private val conversationRepository = FakeConversationRepository()
    private val userRepository = FakeUserRepository()
    private val authRepository = FakeAuthRepository().apply {
        loggedInUser = User(id = "me")
        authState.value = DomainUser(id = "me")
    }
    private val me = mockk<FirebaseUser> { every { uid } returns "me" }
    private val firebaseAuth = mockk<FirebaseAuth> { every { currentUser } returns me }

    private fun createViewModel() = ChatsViewModel(
        getConversationsUseCase = GetConversationsUseCase(conversationRepository),
        deleteConversationUseCase = DeleteConversationUseCase(conversationRepository),
        deleteConversationForEveryoneUseCase = DeleteConversationForEveryoneUseCase(conversationRepository),
        clearConversationForMeUseCase = ClearConversationForMeUseCase(conversationRepository),
        markConversationAsReadUseCase = MarkConversationAsReadUseCase(conversationRepository),
        markConversationAsUnreadUseCase = MarkConversationAsUnreadUseCase(conversationRepository),
        createConversationUseCase = CreateConversationUseCase(
            conversationRepository,
            GetCurrentUserUseCase(authRepository),
        ),
        syncConversationsUseCase = SyncConversationsUseCase(conversationRepository),
        observeUserPresenceUseCase = ObserveUserPresenceUseCase(FakePresenceService()),
        observeAuthStateUseCase = ObserveAuthStateUseCase(authRepository),
        userRepository = userRepository,
        firebaseAuth = firebaseAuth,
    )

    private fun conversation(id: String, partnerAvatar: String? = null) = Conversation(
        id = id,
        participantIds = listOf("me", "bob"),
        participantAvatars = listOf(null, partnerAvatar),
    )

    @Test
    fun `signed-out user sees an empty list`() = runTest {
        authRepository.authState.value = null
        conversationRepository.conversations.value = listOf(conversation("c1"))

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.conversations).isEmpty()
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `conversations arrive and loading clears`() = runTest {
        conversationRepository.conversations.value = listOf(conversation("c1"))

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.conversations.map { it.id }).containsExactly("c1")
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `partner avatars are seeded from the conversation, then fetched once per user`() = runTest {
        userRepository.usersById["bob"] = User(id = "bob", avatarUrl = "fresh.png")
        conversationRepository.conversations.value = listOf(conversation("c1", partnerAvatar = "seed.png"))
        val viewModel = createViewModel()

        conversationRepository.conversations.value = listOf(
            conversation("c1", partnerAvatar = "seed.png"),
            conversation("c2", partnerAvatar = "seed.png"),
        )

        assertThat(viewModel.uiState.value.avatars["bob"]).isEqualTo("fresh.png")
        assertThat(userRepository.requestedUserIds).containsExactly("bob")
    }

    @Test
    fun `delete for me failure surfaces the error`() = runTest {
        conversationRepository.conversations.value = listOf(conversation("c1"))
        val viewModel = createViewModel()
        conversationRepository.failWith = Exception("nope")

        viewModel.deleteForMe("c1")

        assertThat(viewModel.uiState.value.error).isEqualTo(UiText.DynamicString("nope"))
    }

    @Test
    fun `refresh syncs once and clears isRefreshing`() = runTest {
        val viewModel = createViewModel()

        viewModel.refresh()

        assertThat(conversationRepository.syncCount).isEqualTo(1)
        assertThat(viewModel.uiState.value.isRefreshing).isFalse()
    }
}
