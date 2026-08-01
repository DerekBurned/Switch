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

class GetUserByIdUseCaseTest {

    private val repository = FakeUserRepository()
    private val useCase = GetUserByIdUseCase(repository)

    @Test
    fun `blank id emits loading then invalid id error`() = runTest {
        useCase("").test {
            assertThat(awaitItem()).isEqualTo(Resource.Loading)
            val error = awaitItem()
            assertThat(error).isInstanceOf(Resource.Error::class)
            assertThat((error as Resource.Error).message).isEqualTo("Invalid user ID")
            awaitComplete()
        }
    }

    @Test
    fun `existing user is emitted as success`() = runTest {
        val user = User(id = "u1", username = "bob")
        repository.usersById["u1"] = user

        useCase("u1").test {
            assertThat(awaitItem()).isEqualTo(Resource.Loading)
            assertThat(awaitItem()).isEqualTo(Resource.Success(user))
            awaitComplete()
        }
    }

    @Test
    fun `missing user is emitted as success with null data`() = runTest {
        useCase("ghost").test {
            assertThat(awaitItem()).isEqualTo(Resource.Loading)
            assertThat(awaitItem()).isEqualTo(Resource.Success<User?>(null))
            awaitComplete()
        }
    }

    @Test
    fun `repository failure is mapped to an error`() = runTest {
        repository.failWith = IllegalStateException("boom")

        useCase("u1").test {
            assertThat(awaitItem()).isEqualTo(Resource.Loading)
            val error = awaitItem()
            assertThat(error).isInstanceOf(Resource.Error::class)
            assertThat((error as Resource.Error).message).isEqualTo("boom")
            awaitComplete()
        }
    }
}
