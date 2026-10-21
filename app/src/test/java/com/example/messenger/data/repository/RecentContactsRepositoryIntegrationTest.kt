package com.example.messenger.data.repository

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.example.messenger.data.local.obx.ObxRecentContact
import com.example.messenger.domain.model.User
import com.example.messenger.testutil.ObjectBoxExtension
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class RecentContactsRepositoryIntegrationTest {

    @JvmField
    @RegisterExtension
    val obx = ObjectBoxExtension()

    private val repository by lazy { RecentContactsRepository(obx.box()) }

    @Test
    fun `opening the same contact twice keeps one entry with the newest name`() = runTest {
        repository.recordOpened("bob", "Bob", null)
        repository.recordOpened("bob", "Robert", "robert.png")

        assertThat(obx.box<ObxRecentContact>().count()).isEqualTo(1L)
        assertThat(repository.observeRecentOpened().first())
            .containsExactly(User(id = "bob", username = "Robert", avatarUrl = "robert.png"))
    }

    @Test
    fun `a blank name does not erase the stored one`() = runTest {
        repository.recordOpened("bob", "Bob", "bob.png")
        repository.recordOpened("bob", "", null)

        assertThat(repository.observeRecentOpened().first())
            .containsExactly(User(id = "bob", username = "Bob", avatarUrl = "bob.png"))
    }

    @Test
    fun `recently opened lists newest first and stops at the limit`() = runTest {
        (1..10).forEach { i ->
            obx.box<ObxRecentContact>().put(
                ObxRecentContact(userId = "u$i", username = "U$i", lastOpenedAt = i.toLong()),
            )
        }
        obx.box<ObxRecentContact>().put(ObxRecentContact(userId = "searched", lastSearchedAt = 99))

        val opened = repository.observeRecentOpened(limit = 8).first()

        assertThat(opened.map { it.id })
            .containsExactly("u10", "u9", "u8", "u7", "u6", "u5", "u4", "u3")
    }

    @Test
    fun `blank user id is ignored`() = runTest {
        repository.recordOpened("", "Nobody", null)
        repository.recordSearched(User(id = "", username = "Nobody"))

        assertThat(obx.box<ObxRecentContact>().count()).isEqualTo(0L)
    }
}
