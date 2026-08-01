package com.example.messenger.domain.usecase.conversation

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.message
import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.model.User
import com.example.messenger.domain.usecase.auth.GetCurrentUserUseCase
import com.example.messenger.testutil.fakes.FakeAuthRepository
import com.example.messenger.testutil.fakes.FakeConversationRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CreateConversationUseCaseTest {

    private val authRepository = FakeAuthRepository()
    private val conversationRepository = FakeConversationRepository()
    private val useCase = CreateConversationUseCase(
        conversationRepository = conversationRepository,
        getCurrentUserUseCase = GetCurrentUserUseCase(authRepository),
    )

    @Test
    fun `fails when there is no logged-in user`() = runTest {
        authRepository.loggedInUser = null

        val result = useCase("other")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isNotNull().message().isEqualTo("User not logged in")
    }

    @Test
    fun `fails on a blank participant id`() = runTest {
        authRepository.loggedInUser = User(id = "me")

        val result = useCase("   ")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isNotNull().message().isEqualTo("Invalid participant ID")
    }

    @Test
    fun `fails when trying to start a conversation with yourself`() = runTest {
        authRepository.loggedInUser = User(id = "me")

        val result = useCase("me")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isNotNull()
            .message().isEqualTo("Cannot create conversation with yourself")
    }

    @Test
    fun `creates a conversation with both participant ids`() = runTest {
        authRepository.loggedInUser = User(id = "me")
        conversationRepository.createdConversation = Conversation(id = "conv42")

        val result = useCase("other")

        assertThat(result.isFailure).isFalse()
        assertThat(result.getOrNull()).isEqualTo(Conversation(id = "conv42"))
        assertThat(conversationRepository.createdParticipants).containsExactly(listOf("me", "other"))
    }
}
