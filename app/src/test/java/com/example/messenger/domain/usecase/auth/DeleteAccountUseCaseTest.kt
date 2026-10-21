package com.example.messenger.domain.usecase.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.example.messenger.domain.model.User
import com.example.messenger.testutil.fakes.FakeAuthRepository
import com.example.messenger.testutil.fakes.FakeLocalRepository
import com.example.messenger.testutil.fakes.FakeSessionCleaner
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeleteAccountUseCaseTest {

    private val authRepository = FakeAuthRepository()
    private val localRepository = FakeLocalRepository()
    private val sessionCleaner = FakeSessionCleaner()
    private val useCase = DeleteAccountUseCase(authRepository, localRepository, sessionCleaner)

    @BeforeEach
    fun signIn() {
        authRepository.loggedInUser = User(id = "u1", username = "bob")
    }

    @Test
    fun `successful deletion wipes the device and signs out`() = runTest {
        val result = useCase()

        assertThat(result.isSuccess).isTrue()
        assertThat(authRepository.deleteCalls).isEqualTo(1)
        assertThat(sessionCleaner.deletedUid).isEqualTo("u1")
        assertThat(localRepository.resetCount).isEqualTo(1)
        assertThat(authRepository.loggedOut).isTrue()
    }

    @Test
    fun `a refused server deletion leaves the session intact`() = runTest {
        authRepository.deleteResult = Result.failure(IllegalStateException("nope"))

        val result = useCase()

        assertThat(result.isFailure).isTrue()
        assertThat(sessionCleaner.deletedUid).isNull()
        assertThat(sessionCleaner.deleteCalls).isEqualTo(0)
        assertThat(localRepository.resetCount).isEqualTo(0)
        assertThat(authRepository.loggedOut).isFalse()
    }

    @Test
    fun `deletion is safe to retry after a failure`() = runTest {
        authRepository.deleteResult = Result.failure(IllegalStateException("offline"))
        assertThat(useCase().isFailure).isTrue()

        authRepository.deleteResult = Result.success(Unit)
        assertThat(useCase().isSuccess).isTrue()

        assertThat(authRepository.deleteCalls).isEqualTo(2)
        assertThat(sessionCleaner.deleteCalls).isEqualTo(1)
        assertThat(authRepository.loggedOut).isTrue()
    }
}
