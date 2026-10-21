package com.example.messenger.domain.usecase.auth

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.example.messenger.domain.model.DomainUser
import com.example.messenger.domain.model.RemoteUserSnapshot
import com.example.messenger.domain.model.SessionEndReason
import com.example.messenger.domain.model.SessionState
import com.example.messenger.domain.model.User
import com.example.messenger.testutil.fakes.FakeAuthFlowGate
import com.example.messenger.testutil.fakes.FakeAuthRepository
import com.example.messenger.testutil.fakes.FakeUserRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ObserveSessionStateUseCaseTest {

    private val authRepository = FakeAuthRepository()
    private val userRepository = FakeUserRepository()
    private val gate = FakeAuthFlowGate()
    private val useCase = ObserveSessionStateUseCase(
        observeAuthState = ObserveAuthStateUseCase(authRepository),
        userRepository = userRepository,
        authFlowGate = gate,
    )

    private val signedIn = DomainUser(id = "u1", displayName = "bob", photoUrl = "https://a/1.jpg")
    private val profile = User(id = "u1", username = "bob")

    @Test
    fun `no firebase user is signed out with no reason`() = runTest {
        useCase().test {
            assertThat(awaitItem()).isEqualTo(SessionState.SignedOut())
        }
    }

    @Test
    fun `existing profile resolves to active`() = runTest {
        userRepository.remoteUsers.value = RemoteUserSnapshot(user = profile, authoritative = true)

        useCase().test {
            assertThat(awaitItem()).isEqualTo(SessionState.SignedOut())
            authRepository.authState.value = signedIn
            assertThat(awaitItem()).isEqualTo(SessionState.Resolving)
            assertThat(awaitItem()).isEqualTo(SessionState.Active(profile))
        }
    }

    @Test
    fun `a tombstoned profile signs out as a deleted account`() = runTest {
        userRepository.remoteUsers.value = RemoteUserSnapshot(
            user = profile.copy(deleted = true, deletedAt = 1L),
            authoritative = true,
        )

        useCase().test {
            assertThat(awaitItem()).isEqualTo(SessionState.SignedOut())
            authRepository.authState.value = signedIn
            assertThat(awaitItem()).isEqualTo(SessionState.Resolving)
            assertThat(awaitItem())
                .isEqualTo(SessionState.SignedOut(SessionEndReason.ACCOUNT_DELETED))
        }
    }

    @Test
    fun `server confirmed missing profile signs out`() = runTest {
        userRepository.remoteUsers.value = RemoteUserSnapshot(user = null, authoritative = true)

        useCase().test {
            assertThat(awaitItem()).isEqualTo(SessionState.SignedOut())
            authRepository.authState.value = signedIn
            assertThat(awaitItem()).isEqualTo(SessionState.Resolving)
            assertThat(awaitItem())
                .isEqualTo(SessionState.SignedOut(SessionEndReason.PROFILE_MISSING))
        }
    }

    @Test
    fun `cached miss keeps the session active on the firebase identity`() = runTest {
        userRepository.remoteUsers.value = RemoteUserSnapshot(user = null, authoritative = false)

        useCase().test {
            assertThat(awaitItem()).isEqualTo(SessionState.SignedOut())
            authRepository.authState.value = signedIn
            assertThat(awaitItem()).isEqualTo(SessionState.Resolving)
            assertThat(awaitItem()).isEqualTo(
                SessionState.Active(
                    User(
                        id = "u1",
                        username = "bob",
                        usernameLower = "bob",
                        avatarUrl = "https://a/1.jpg",
                    ),
                ),
            )
        }
    }

    @Test
    fun `profile removed while active signs out`() = runTest {
        userRepository.remoteUsers.value = RemoteUserSnapshot(user = profile, authoritative = true)
        authRepository.authState.value = signedIn

        useCase().test {
            assertThat(awaitItem()).isEqualTo(SessionState.Resolving)
            assertThat(awaitItem()).isEqualTo(SessionState.Active(profile))

            userRepository.remoteUsers.value = RemoteUserSnapshot(user = null, authoritative = true)
            assertThat(awaitItem())
                .isEqualTo(SessionState.SignedOut(SessionEndReason.PROFILE_MISSING))
        }
    }

    @Test
    fun `an in flight auth flow suppresses the missing profile sign out`() = runTest {
        userRepository.remoteUsers.value = RemoteUserSnapshot(user = null, authoritative = true)
        gate.busy.value = true
        authRepository.authState.value = signedIn

        useCase().test {
            assertThat(awaitItem()).isEqualTo(SessionState.Resolving)
            expectNoEvents()

            userRepository.remoteUsers.value = RemoteUserSnapshot(user = profile, authoritative = true)
            gate.busy.value = false
            assertThat(awaitItem()).isEqualTo(SessionState.Active(profile))
        }
    }
}
