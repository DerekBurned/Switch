package com.example.messenger.domain.usecase.message

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.voicemessage.Clip
import com.example.messenger.testutil.fakes.FakeClipRepository
import com.example.messenger.testutil.fakes.FakeMessageRepository
import com.example.messenger.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class DeleteMessageUseCaseTest {

    private val messages = FakeMessageRepository()
    private val clips = FakeClipRepository()
    private val useCase = DeleteMessageUseCase(messages, clips)

    private val voice = Message.Clip(
        id = "m1",
        conversationId = "c1",
        senderId = "me",
        clip = Clip.Voice(url = "https://storage.example/clip.m4a"),
    )

    @Test
    fun `deleting a voice clip for everyone removes its uploaded file`() = runTest {
        useCase(voice, forEveryone = true).test {
            assertThat(awaitItem()).isEqualTo(Resource.Loading)
            assertThat(awaitItem()).isInstanceOf(Resource.Success::class)
            awaitComplete()
        }
        assertThat(clips.remoteDeleted).containsExactly("m1")
    }

    @Test
    fun `deleting a voice clip for me keeps the uploaded file`() = runTest {
        useCase(voice, forEveryone = false).test {
            assertThat(awaitItem()).isEqualTo(Resource.Loading)
            assertThat(awaitItem()).isInstanceOf(Resource.Success::class)
            awaitComplete()
        }
        assertThat(clips.remoteDeleted).isEmpty()
    }

    @Test
    fun `deleting a text message never touches clip storage`() = runTest {
        useCase(Message.Text(id = "t1", senderId = "me", text = "hi"), forEveryone = true).test {
            assertThat(awaitItem()).isEqualTo(Resource.Loading)
            assertThat(awaitItem()).isInstanceOf(Resource.Success::class)
            awaitComplete()
        }
        assertThat(clips.remoteDeleted).isEmpty()
    }

    @Test
    fun `a failed delete keeps the uploaded file`() = runTest {
        messages.deleteResult = Result.failure(IllegalStateException("offline"))
        useCase(voice, forEveryone = true).test {
            assertThat(awaitItem()).isEqualTo(Resource.Loading)
            assertThat(awaitItem()).isInstanceOf(Resource.Error::class)
            awaitComplete()
        }
        assertThat(clips.remoteDeleted).isEmpty()
    }
}
