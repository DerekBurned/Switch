package com.example.messenger.domain.usecase.message

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.User
import com.example.messenger.domain.usecase.auth.GetCurrentUserUseCase
import com.example.messenger.testutil.fakes.FakeAuthRepository
import com.example.messenger.testutil.fakes.FakeMessageRepository
import com.example.messenger.util.Resource
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SendMessageUseCaseTest {

    private val messageRepository = FakeMessageRepository()
    private val authRepository = FakeAuthRepository().apply { loggedInUser = User(id = "me") }
    private val sendMessage = SendMessageUseCase(messageRepository, GetCurrentUserUseCase(authRepository))

    @Test
    fun `blank text emits an error and sends nothing`() = runTest {
        val emissions = sendMessage("c1", "   ").toList()

        assertThat(emissions).containsExactly(Resource.Error("Message is empty"))
        assertThat(messageRepository.sent).isEmpty()
    }

    @Test
    fun `text over 5000 characters emits an error and sends nothing`() = runTest {
        val emissions = sendMessage("c1", "a".repeat(5001)).toList()

        assertThat(emissions).containsExactly(Resource.Error("Message is too long"))
        assertThat(messageRepository.sent).isEmpty()
    }

    @Test
    fun `exactly 5000 characters is sent`() = runTest {
        val emissions = sendMessage("c1", "a".repeat(5000)).toList()

        assertThat(emissions).containsExactly(Resource.Success(Unit))
        val sent = messageRepository.sent.single() as Message.Text
        assertThat(sent.text.length).isEqualTo(5000)
    }

    @Test
    fun `signed-out user gets an error and nothing is sent`() = runTest {
        authRepository.loggedInUser = null

        val emissions = sendMessage("c1", "hi").toList()

        assertThat(emissions).containsExactly(Resource.Error("User not logged in"))
        assertThat(messageRepository.sent).isEmpty()
    }

    @Test
    fun `message is trimmed, SENDING, and carries the reply snippet`() = runTest {
        val replyTo = Message.Text(id = "r1", senderId = "bob", text = "orig")

        val emissions = sendMessage("c1", "  hi  ", replyTo).toList()

        assertThat(emissions).containsExactly(Resource.Success(Unit))
        val sent = messageRepository.sent.single() as Message.Text
        assertThat(sent.conversationId).isEqualTo("c1")
        assertThat(sent.senderId).isEqualTo("me")
        assertThat(sent.text).isEqualTo("hi")
        assertThat(sent.status).isEqualTo(MessageStatus.SENDING)
        assertThat(sent.replyToMessageId).isEqualTo("r1")
        assertThat(sent.replyToSenderId).isEqualTo("bob")
        assertThat(sent.replyToText).isEqualTo("orig")
    }

    @Test
    fun `repository failure emits its message`() = runTest {
        messageRepository.sendResult = Result.failure(Exception("offline"))

        val emissions = sendMessage("c1", "hi").toList()

        assertThat(emissions).containsExactly(Resource.Error("offline"))
    }
}
