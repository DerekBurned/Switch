package com.example.messenger.domain.usecase.message

import assertk.assertThat
import assertk.assertions.containsExactly
import com.example.messenger.testutil.fakes.FakeMessageRepository
import com.example.messenger.util.Resource
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SyncMessagesUseCaseTest {

    private val repository = FakeMessageRepository()
    private val syncMessages = SyncMessagesUseCase(repository)

    @Test
    fun `each sync result maps to Success or Error`() = runTest {
        repository.recentResults = flowOf(Result.success(Unit), Result.failure(Exception("x")))

        assertThat(syncMessages("c1").toList())
            .containsExactly(Resource.Loading, Resource.Success(Unit), Resource.Error("x"))
    }

    @Test
    fun `a failure without a message reads Sync failed`() = runTest {
        repository.recentResults = flowOf(Result.failure(Exception()))

        assertThat(syncMessages("c1").toList())
            .containsExactly(Resource.Loading, Resource.Error("Sync failed"))
    }
}
