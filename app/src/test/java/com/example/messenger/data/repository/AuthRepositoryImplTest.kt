package com.example.messenger.data.repository

import assertk.assertThat
import assertk.assertions.isInstanceOf
import com.example.messenger.data.crypto.E2eeBootstrap
import com.example.messenger.data.remote.auth.AccountDeletionService
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.data.remote.firebase.FirestoreService
import com.example.messenger.domain.model.AuthAccountException
import com.example.messenger.domain.model.User
import com.example.messenger.testutil.AndroidLogExtension
import com.example.messenger.testutil.fakes.FakeAuthFlowGate
import com.example.messenger.util.Resource
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class AuthRepositoryImplTest {

    @JvmField
    @RegisterExtension
    val log = AndroidLogExtension()

    private val credential = mockk<PhoneAuthCredential>()
    private val firebaseUser = mockk<FirebaseUser> {
        every { uid } returns "uid-1"
        every { displayName } returns null
        every { photoUrl } returns null
    }
    private val auth = mockk<FirebaseAuthService>(relaxed = true) {
        coEvery { signInWithPhone(credential) } returns Result.success(firebaseUser)
    }
    private val firestore = mockk<FirestoreService>(relaxed = true)
    private val e2eeBootstrap = mockk<E2eeBootstrap>(relaxed = true)

    private val repository = AuthRepositoryImpl(
        auth = auth,
        firestore = firestore,
        e2eeBootstrap = e2eeBootstrap,
        authFlowGate = FakeAuthFlowGate(),
        accountDeletion = mockk<AccountDeletionService>(relaxed = true),
    )

    private fun profileExists(exists: Boolean) {
        coEvery { firestore.getUserProfile("uid-1") } returns if (exists) {
            Result.success(User(id = "uid-1", username = "tester"))
        } else {
            Result.failure(NoSuchElementException())
        }
    }

    @Test
    fun `registering a number that already has a profile fails as AlreadyRegistered and signs out`() = runTest {
        profileExists(true)

        val result = repository.loginWithPhone(credential, "tester", isRegistration = true)

        assertThat((result as Resource.Failure).exception).isInstanceOf(AuthAccountException.AlreadyRegistered::class)
        coVerify { auth.logout() }
        coVerify(exactly = 0) { firestore.createUserProfile(any()) }
    }

    @Test
    fun `logging in with a number that has no profile fails as NotRegistered and deletes the auth user`() = runTest {
        profileExists(false)
        coEvery { auth.deleteAccount() } returns Result.success(Unit)

        val result = repository.loginWithPhone(credential, null, isRegistration = false)

        assertThat((result as Resource.Failure).exception).isInstanceOf(AuthAccountException.NotRegistered::class)
        coVerify { auth.deleteAccount() }
        coVerify(exactly = 0) { auth.logout() }
    }

    @Test
    fun `when deleting the orphan auth user fails, NotRegistered still signs out`() = runTest {
        profileExists(false)
        coEvery { auth.deleteAccount() } returns Result.failure(IllegalStateException("network"))

        val result = repository.loginWithPhone(credential, null, isRegistration = false)

        assertThat((result as Resource.Failure).exception).isInstanceOf(AuthAccountException.NotRegistered::class)
        coVerify { auth.logout() }
    }
}
