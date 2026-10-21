package com.example.messenger.domain.usecase.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.example.messenger.domain.model.User
import com.example.messenger.testutil.fakes.FakeAuthRepository
import com.example.messenger.testutil.fakes.FakeLocalRepository
import com.example.messenger.testutil.fakes.FakeSessionCleaner
import com.example.messenger.testutil.fakes.FakeUserRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class LogoutUseCaseTest {

    private val authRepository = FakeAuthRepository()
    private val userRepository = FakeUserRepository()
    private val localRepository = FakeLocalRepository()
    private val sessionCleaner = FakeSessionCleaner()
    private val useCase = LogoutUseCase(
        authRepository,
        userRepository,
        localRepository,
        sessionCleaner,
    )

    @Test
    fun `the device session is cleared for the signed-in user`() = runTest {
        authRepository.loggedInUser = User(id = "u1", username = "bob")

        useCase()

        assertThat(sessionCleaner.loggedOutUid).isEqualTo("u1")
        assertThat(authRepository.loggedOut).isTrue()
        assertThat(localRepository.resetCount).isEqualTo(1)
    }

    @Test
    fun `the local cache is wiped exactly once`() = runTest {
        authRepository.loggedInUser = User(id = "u1")

        useCase()

        assertThat(localRepository.resetCount).isEqualTo(1)
    }

    @Test
    fun `logging out with no signed-in user still tears the session down`() = runTest {
        authRepository.loggedInUser = null

        useCase()

        assertThat(sessionCleaner.logoutCalls).isEqualTo(1)
        assertThat(sessionCleaner.loggedOutUid).isNull()
        assertThat(localRepository.resetCount).isEqualTo(1)
    }

    @Test
    fun `a failure marking the user offline does not block sign-out`() = runTest {
        authRepository.loggedInUser = User(id = "u1")
        userRepository.failWith = IllegalStateException("offline")

        useCase()

        assertThat(authRepository.loggedOut).isTrue()
        assertThat(localRepository.resetCount).isEqualTo(1)
    }
}
