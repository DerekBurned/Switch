package com.example.messenger.data.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.example.messenger.data.local.obx.ObxContactAlias
import com.example.messenger.data.local.obx.ObxUser
import com.example.messenger.data.local.obx.mappers.toObx
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.data.remote.firebase.FirebaseStorageService
import com.example.messenger.data.remote.firebase.FirestoreService
import com.example.messenger.domain.model.RemoteUserSnapshot
import com.example.messenger.domain.model.User
import com.example.messenger.testutil.AndroidLogExtension
import com.example.messenger.testutil.ObjectBoxExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class UserRepositoryImplIntegrationTest {

    @JvmField
    @RegisterExtension
    val log = AndroidLogExtension()

    @JvmField
    @RegisterExtension
    val obx = ObjectBoxExtension()

    private val firestore = mockk<FirestoreService>()
    private val auth = mockk<FirebaseAuthService> { every { getCurrentUserId() } returns "me" }

    private val repository by lazy {
        UserRepositoryImpl(
            userBox = obx.box(),
            conversationBox = obx.box(),
            contactAliasBox = obx.box(),
            firestoreService = firestore,
            authService = auth,
            storageService = mockk<FirebaseStorageService>(),
        )
    }

    private fun cacheUser(id: String, username: String) {
        obx.box<ObxUser>().put(User(id = id, username = username).toObx())
    }

    private fun storedUser(id: String): ObxUser = obx.box<ObxUser>().all.single { it.uid == id }

    @Test
    fun `renaming a contact trims the name, caches it, and the alias stream emits it`() = runTest {
        coEvery { firestore.setContactAlias("me", "c1", "Bobby") } returns Result.success(Unit)

        val result = repository.updateContactName("c1", "  Bobby  ")

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.observeContactAliases().first()).isEqualTo(mapOf("c1" to "Bobby"))
        coVerify { firestore.setContactAlias("me", "c1", "Bobby") }
    }

    @Test
    fun `a blank contact name is rejected and nothing is cached`() = runTest {
        val result = repository.updateContactName("c1", "   ")

        assertThat(result.exceptionOrNull()).isNotNull().isInstanceOf(IllegalArgumentException::class)
        assertThat(obx.box<ObxContactAlias>().count()).isEqualTo(0L)
        coVerify(exactly = 0) { firestore.setContactAlias(any(), any(), any()) }
    }

    @Test
    fun `refreshing aliases caches every alias the server returns`() = runTest {
        coEvery { firestore.getContactAliases("me") } returns Result.success(mapOf("a" to "Ann", "b" to "Ben"))

        repository.refreshContactAliases()

        assertThat(repository.observeContactAliases().first())
            .isEqualTo(mapOf("a" to "Ann", "b" to "Ben"))
    }

    @Test
    fun `a user missing on the server is cached as deleted`() = runTest {
        cacheUser("bob", "Bob")
        coEvery { firestore.fetchRemoteUser("bob") } returns Result.success(
            RemoteUserSnapshot(user = null, authoritative = true),
        )

        val result = repository.getUserById("bob")

        assertThat(result.getOrNull()?.deleted).isEqualTo(true)
        assertThat(storedUser("bob").deleted).isTrue()
    }

    @Test
    fun `an unreachable server returns the cached user`() = runTest {
        cacheUser("bob", "Bob")
        coEvery { firestore.fetchRemoteUser("bob") } returns Result.failure(Exception("offline"))

        val result = repository.getUserById("bob")

        assertThat(result.getOrNull()?.username).isEqualTo("Bob")
        assertThat(storedUser("bob").deleted).isFalse()
    }

    @Test
    fun `a fresh server copy replaces the cached user`() = runTest {
        cacheUser("bob", "Bob")
        coEvery { firestore.fetchRemoteUser("bob") } returns Result.success(
            RemoteUserSnapshot(user = User(id = "bob", username = "Robert"), authoritative = true),
        )

        repository.getUserById("bob")

        assertThat(obx.box<ObxUser>().count()).isEqualTo(1L)
        assertThat(storedUser("bob").username).isEqualTo("Robert")
    }
}
