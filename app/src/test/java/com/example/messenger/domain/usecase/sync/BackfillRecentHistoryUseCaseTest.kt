package com.example.messenger.domain.usecase.sync

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.example.messenger.domain.model.Conversation
import com.example.messenger.testutil.fakes.FakeConversationRepository
import com.example.messenger.testutil.fakes.FakeMessageRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class BackfillRecentHistoryUseCaseTest {

    private val conversationRepository = FakeConversationRepository()
    private val messageRepository = FakeMessageRepository()
    private val useCase = BackfillRecentHistoryUseCase(conversationRepository, messageRepository)

    private fun conversations(count: Int) = (1..count).map { index ->
        Conversation(id = "c$index", lastMessageTimestamp = index.toLong())
    }

    @Test
    fun `conversations are synced from the server before any backfill`() = runTest {
        conversationRepository.conversations.value = conversations(2)

        useCase()

        assertThat(conversationRepository.syncCount).isEqualTo(1)
    }

    @Test
    fun `only the most recent conversations are backfilled`() = runTest {
        conversationRepository.conversations.value = conversations(35)

        useCase()

        assertThat(messageRepository.backfilled).hasSize(30)
        assertThat(messageRepository.backfilled.map { it.first }.take(3))
            .containsExactly("c35", "c34", "c33")
    }

    @Test
    fun `each conversation is backfilled with the message limit`() = runTest {
        conversationRepository.conversations.value = conversations(1)

        useCase()

        assertThat(messageRepository.backfilled.single().second)
            .isEqualTo(BackfillRecentHistoryUseCase.MESSAGE_LIMIT)
    }

    @Test
    fun `the pulled message count is returned`() = runTest {
        conversationRepository.conversations.value = conversations(3)
        messageRepository.backfillResult = { Result.success(50) }

        assertThat(useCase().getOrNull()).isEqualTo(150)
    }

    @Test
    fun `one failing conversation does not abort the rest`() = runTest {
        conversationRepository.conversations.value = conversations(3)
        messageRepository.backfillResult = { id ->
            if (id == "c2") Result.failure(IllegalStateException("boom")) else Result.success(10)
        }

        val result = useCase()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(20)
        assertThat(messageRepository.backfilled).hasSize(3)
    }
}
