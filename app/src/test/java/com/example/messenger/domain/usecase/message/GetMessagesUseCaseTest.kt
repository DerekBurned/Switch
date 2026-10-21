package com.example.messenger.domain.usecase.message

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.example.messenger.domain.model.Message
import com.example.messenger.testutil.fakes.FakeMessageRepository
import com.example.messenger.util.Resource
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class GetMessagesUseCaseTest {

    private val repository = FakeMessageRepository()
    private val getMessages = GetMessagesUseCase(repository)

    @Test
    fun `blank conversation id emits the invalid conversation error`() = runTest {
        assertThat(getMessages(" ").toList()).containsExactly(Resource.Error("Invalid conversation ID"))
    }

    @Test
    fun `stream emits Loading then Success with the messages`() = runTest {
        val message = Message.Text(id = "m1", conversationId = "c1", text = "hi")
        repository.messages.value = listOf(message)

        getMessages("c1").test {
            assertThat(awaitItem()).isEqualTo(Resource.Loading)
            assertThat(awaitItem()).isEqualTo(Resource.Success(listOf(message)))
        }
    }

    @Test
    fun `stream exception becomes an Error with its message`() = runTest {
        repository.messagesError = IllegalStateException("db closed")

        assertThat(getMessages("c1").toList())
            .containsExactly(Resource.Loading, Resource.Error("db closed"))
    }
}
