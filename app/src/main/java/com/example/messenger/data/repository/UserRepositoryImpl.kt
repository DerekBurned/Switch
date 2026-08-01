package com.example.messenger.data.repository

import com.example.messenger.data.local.obx.ObxContactAlias
import com.example.messenger.data.local.obx.ObxContactAlias_
import com.example.messenger.data.local.obx.ObxConversation
import com.example.messenger.data.local.obx.ObxUser
import com.example.messenger.data.local.obx.ObxUser_
import com.example.messenger.data.local.obx.asFlow
import com.example.messenger.data.local.obx.queryFlow
import com.example.messenger.data.local.obx.toDomain
import com.example.messenger.data.local.obx.toObx
import android.net.Uri
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.data.remote.firebase.FirestoreService
import com.example.messenger.data.remote.firebase.FirebaseStorageService
import com.example.messenger.domain.model.PhoneNumber
import com.example.messenger.domain.model.User
import com.example.messenger.domain.repository.IUserRepository
import com.example.messenger.util.similarityScore
import io.objectbox.Box
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userBox: Box<ObxUser>,
    private val conversationBox: Box<ObxConversation>,
    private val contactAliasBox: Box<ObxContactAlias>,
    private val firestoreService: FirestoreService,
    private val authService: FirebaseAuthService,
    private val storageService: FirebaseStorageService,
) : IUserRepository {

    override suspend fun uploadAvatar(imageUri: Uri): Result<String> {
        return try {
            val uid = authService.getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
            val url = storageService.uploadProfileImage(uid, imageUri).getOrThrow()
            updateUserProfile(mapOf("avatarUrl" to url)).getOrThrow()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProfilePhotos(userId: String): Result<List<String>> {
        return storageService.listProfileImages(userId)
    }

    override suspend fun getUserById(userId: String): Result<User?> {
        return try {
            val local = findByUid(userId)?.toDomain()
            val remote = firestoreService.getUserProfile(userId).getOrNull()
            if (remote != null) {
                upsert(remote.toObx())
                Result.success(remote)
            } else {
                Result.success(local)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val currentUserId = authService.getCurrentUserId()
            val q = query.trim()

            val remote = firestoreService.searchUsers(q).getOrElse { emptyList() }
                .filter { it.id != currentUserId }
            if (remote.isNotEmpty()) {
                userBox.put(remote.map { it.toObx() })
            }

            val aliases = contactAliasBox.all.associate { it.contactId to it.name }

            val local = userBox.all
                .filter {
                    it.uid != currentUserId &&
                        (it.username.contains(q, ignoreCase = true) ||
                            aliases[it.uid]?.contains(q, ignoreCase = true) == true)
                }
                .map { it.toDomain() }

            val ranked = (remote + local)
                .distinctBy { it.id }
                .map { user ->
                    val aliasScore = aliases[user.id]?.let { similarityScore(q, it) } ?: 0.0
                    user to maxOf(similarityScore(q, user.username.orEmpty()), aliasScore)
                }
                .filter { it.second >= SIMILARITY_THRESHOLD }
                .sortedByDescending { it.second }
                .map { it.first }

            Result.success(ranked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeUser(userId: String): Flow<User?> {
        return queryFlow { userBox.query(ObxUser_.uid.equal(userId)).build() }
            .map { rows -> rows.firstOrNull()?.toDomain() }
    }

    override suspend fun updateUserStatus(isOnline: Boolean): Result<Unit> {
        return try {
            val uid = authService.getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
            firestoreService.updateUserProfile(uid, mapOf("isOnline" to isOnline))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLastSeen(): Result<Unit> {
        return try {
            val uid = authService.getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
            firestoreService.updateUserProfile(uid, mapOf("lastSeen" to System.currentTimeMillis()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserProfile(updates: Map<String, Any>): Result<Unit> {
        return try {
            val uid = authService.getCurrentUserId() ?: return Result.failure(Exception("Not logged in"))
            val result = firestoreService.updateUserProfile(uid, updates)
            result.onSuccess {
                findByUid(uid)?.let { local ->
                    updates.forEach { (key, value) ->
                        when (key) {
                            "username" -> local.username = value as String
                            "usernameLower" -> local.usernameLower = value as String
                            "email" -> local.email = value as? String
                            "avatarUrl" -> local.avatarUrl = value as? String
                            "phoneNumber" -> local.phoneNumber = value as? PhoneNumber
                        }
                    }
                    userBox.put(local)
                }
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateContactName(contactId: String, newName: String): Result<Unit> = try {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            Result.failure(IllegalArgumentException("Name cannot be empty"))
        } else {
            val myUid = authService.getCurrentUserId()
                ?: return@updateContactName Result.failure(IllegalStateException("Not logged in"))
            cacheAlias(contactId, trimmed)
            firestoreService.setContactAlias(myUid, contactId, trimmed)
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun observeContactAliases(): Flow<Map<String, String>> =
        queryFlow { contactAliasBox.query().build() }
            .map { rows -> rows.associate { it.contactId to it.name } }

    override suspend fun refreshContactAliases(): Result<Unit> {
        return try {
            val myUid = authService.getCurrentUserId()
                ?: return Result.failure(IllegalStateException("Not logged in"))
            firestoreService.getContactAliases(myUid).onSuccess { aliases ->
                aliases.forEach { (contactId, name) -> cacheAlias(contactId, name) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cacheAlias(contactId: String, name: String) {
        val existing = contactAliasBox.query(ObxContactAlias_.contactId.equal(contactId)).build()
            .use { it.findFirst() }
        contactAliasBox.put(
            ObxContactAlias(
                boxId = existing?.boxId ?: 0,
                contactId = contactId,
                name = name,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    private fun findByUid(uid: String): ObxUser? =
        userBox.query(ObxUser_.uid.equal(uid)).build().use { it.findFirst() }

    private fun upsert(user: ObxUser) {
        findByUid(user.uid)?.let { user.boxId = it.boxId }
        userBox.put(user)
    }

    private companion object {
        const val SIMILARITY_THRESHOLD = 0.4
    }
}
