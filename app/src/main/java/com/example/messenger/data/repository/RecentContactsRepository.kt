package com.example.messenger.data.repository

import com.example.messenger.data.local.obx.ObxRecentContact
import com.example.messenger.data.local.obx.ObxRecentContact_
import com.example.messenger.data.local.obx.queryFlow
import com.example.messenger.domain.model.User
import io.objectbox.Box
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentContactsRepository @Inject constructor(
    private val box: Box<ObxRecentContact>,
) {

    suspend fun recordSearched(user: User) {
        val id = user.id
        if (id.isBlank()) return
        upsert(id) {
            username = user.username?.takeIf { it.isNotBlank() } ?: username
            avatarUrl = user.avatarUrl ?: avatarUrl
            lastSearchedAt = System.currentTimeMillis()
        }
    }

    suspend fun recordOpened(userId: String, username: String?, avatarUrl: String?) {
        if (userId.isBlank()) return
        upsert(userId) {
            this.username = username?.takeIf { it.isNotBlank() } ?: this.username
            this.avatarUrl = avatarUrl ?: this.avatarUrl
            lastOpenedAt = System.currentTimeMillis()
        }
    }

    fun observeRecentSearched(limit: Int = RECENT_LIMIT): Flow<List<User>> =
        queryFlow {
            box.query(ObxRecentContact_.lastSearchedAt.greater(0L))
                .orderDesc(ObxRecentContact_.lastSearchedAt)
                .build()
        }.map { rows -> rows.take(limit).map { it.toUser() } }

    fun observeRecentOpened(limit: Int = RECENT_LIMIT): Flow<List<User>> =
        queryFlow {
            box.query(ObxRecentContact_.lastOpenedAt.greater(0L))
                .orderDesc(ObxRecentContact_.lastOpenedAt)
                .build()
        }.map { rows -> rows.take(limit).map { it.toUser() } }

    private suspend fun upsert(userId: String, mutate: ObxRecentContact.() -> Unit) {
        withContext(Dispatchers.IO) {
            val existing = box.query(ObxRecentContact_.userId.equal(userId))
                .build()
                .use { it.findFirst() }
            val entity = existing ?: ObxRecentContact(userId = userId)
            entity.mutate()
            box.put(entity)
        }
    }

    private fun ObxRecentContact.toUser(): User =
        User(id = userId, username = username, avatarUrl = avatarUrl)

    private companion object {
        const val RECENT_LIMIT = 8
    }
}
