package com.example.messenger.presentation.components.search

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import com.example.messenger.data.repository.RecentContactsRepository
import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.model.User
import com.example.messenger.domain.usecase.auth.GetCurrentUserUseCase
import com.example.messenger.domain.usecase.conversation.CreateConversationUseCase
import com.example.messenger.domain.usecase.user.SearchUsersUseCase
import com.example.messenger.testutil.MainDispatcherExtension
import com.example.messenger.testutil.fakes.FakeAuthRepository
import com.example.messenger.testutil.fakes.FakeConversationRepository
import com.example.messenger.testutil.fakes.FakeUserRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class SearchUsersViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val userRepository = FakeUserRepository()
    private val authRepository = FakeAuthRepository()
    private val conversationRepository = FakeConversationRepository()
    private val recentContactsRepository = mockk<RecentContactsRepository>(relaxed = true).apply {
        every { observeRecentSearched(any()) } returns emptyFlow()
        every { observeRecentOpened(any()) } returns emptyFlow()
    }

    private fun createViewModel() = SearchUsersViewModel(
        searchUsersUseCase = SearchUsersUseCase(userRepository),
        createConversationUseCase = CreateConversationUseCase(
            conversationRepository = conversationRepository,
            getCurrentUserUseCase = GetCurrentUserUseCase(authRepository),
        ),
        recentContactsRepository = recentContactsRepository,
    )

    @Test
    fun `blank query clears users without loading`() = runTest {
        val viewModel = createViewModel()

        viewModel.searchUsers("")

        assertThat(viewModel.state.value.users).isEmpty()
        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    @Test
    fun `valid query populates users from the repository`() = runTest {
        val alice = User(id = "u1", username = "alice")
        userRepository.searchResults = listOf(alice)
        val viewModel = createViewModel()

        viewModel.searchUsers("alice")

        assertThat(viewModel.state.value.users).containsExactly(alice)
        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    @Test
    fun `creating a conversation emits the ConversationCreated effect`() = runTest {
        authRepository.loggedInUser = User(id = "me")
        conversationRepository.createdConversation = Conversation(id = "c9")
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.createConversationWithUser(User(id = "other"))
            assertThat(awaitItem())
                .isEqualTo(SearchUsersEffect.ConversationCreated(Conversation(id = "c9")))
        }
    }
}
