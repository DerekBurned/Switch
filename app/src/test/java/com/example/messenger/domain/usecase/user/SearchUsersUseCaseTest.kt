package com.example.messenger.domain.usecase.user

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.example.messenger.domain.model.User
import com.example.messenger.testutil.fakes.FakeUserRepository
import com.example.messenger.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SearchUsersUseCaseTest {

    private val repository = FakeUserRepository()
    private val useCase = SearchUsersUseCase(repository)

    @Test
    fun `blank query emits loading then empty success`() = runTest {
        useCase("   ").test {
            assertThat(awaitItem()).isEqualTo(Resource.Loading)
            assertThat(awaitItem()).isEqualTo(Resource.Success(emptyList<User>()))
            awaitComplete()
        }
    }

    @Test
    fun `query shorter than two characters emits an error`() = runTest {
        useCase("a").test {
            assertThat(awaitItem()).isEqualTo(Resource.Loading)
            val error = awaitItem()
            assertThat(error).isInstanceOf(Resource.Error::class)
            assertThat((error as Resource.Error).message).isEqualTo("Query is too short")
            awaitComplete()
        }
    }

    @Test
    fun `valid query returns repository results`() = runTest {
        val alice = User(id = "u1", username = "alice")
        repository.searchResults = listOf(alice)

        useCase("alice").test {
            assertThat(awaitItem()).isEqualTo(Resource.Loading)
            assertThat(awaitItem()).isEqualTo(Resource.Success(listOf(alice)))
            awaitComplete()
        }
    }

    @Test
    fun `repository failure is mapped to an error carrying its message`() = runTest {
        repository.failWith = RuntimeException("network down")

        useCase("alice").test {
            assertThat(awaitItem()).isEqualTo(Resource.Loading)
            val error = awaitItem()
            assertThat(error).isInstanceOf(Resource.Error::class)
            assertThat((error as Resource.Error).message).isEqualTo("network down")
            awaitComplete()
        }
    }
}
