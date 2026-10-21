package com.example.messenger.domain.usecase.message

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.example.messenger.domain.model.Message
import com.example.messenger.testutil.AndroidLogExtension
import com.example.messenger.testutil.fakes.FakeMessageRepository
import com.example.messenger.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class MarkMessageAsReadUseCaseTest {

    @JvmField
    @RegisterExtension
    val log = AndroidLogExtension()

    private val repository = FakeMessageRepository()
    private val markRead = MarkMessageAsReadUseCase(repository)
    private val message = Message.Text(id = "m1", conversationId = "c1", senderId = "bob")

    @Test
    fun `success maps to Success`() = runTest {
        assertThat(markRead(message)).isEqualTo(Resource.Success(Unit))
        assertThat(repository.markedRead).containsExactly(message)
    }

    @Test
    fun `failure maps to Error with the exception text`() = runTest {
        repository.markReadResult = Result.failure(IllegalStateException("denied"))

        assertThat(markRead(message))
            .isEqualTo(Resource.Error("java.lang.IllegalStateException: denied"))
    }
}
