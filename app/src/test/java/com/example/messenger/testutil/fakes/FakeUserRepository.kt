package com.example.messenger.testutil.fakes

import android.net.Uri
import com.example.messenger.domain.model.User
import com.example.messenger.domain.repository.IUserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserRepository : IUserRepository {

    val usersById = mutableMapOf<String, User>()
    var searchResults: List<User> = emptyList()
    var profilePhotos: List<String> = emptyList()
    val contactAliases = MutableStateFlow<Map<String, String>>(emptyMap())

    val updatedContactNames = mutableListOf<Pair<String, String>>()
    var lastProfileUpdate: Map<String, Any>? = null
    var uploadedAvatarUrl: String = "https://avatar/uploaded"

    var failWith: Throwable? = null

    private fun <T> result(value: T): Result<T> =
        failWith?.let { Result.failure(it) } ?: Result.success(value)

    override suspend fun getUserById(userId: String): Result<User?> = result(usersById[userId])

    override suspend fun searchUsers(query: String): Result<List<User>> = result(searchResults)

    override suspend fun uploadAvatar(imageUri: Uri): Result<String> = result(uploadedAvatarUrl)

    override suspend fun getProfilePhotos(userId: String): Result<List<String>> = result(profilePhotos)

    override fun observeUser(userId: String): Flow<User?> = MutableStateFlow(usersById[userId])

    override suspend fun updateUserStatus(isOnline: Boolean): Result<Unit> = result(Unit)

    override suspend fun updateLastSeen(): Result<Unit> = result(Unit)

    override suspend fun updateUserProfile(updates: Map<String, Any>): Result<Unit> {
        lastProfileUpdate = updates
        return result(Unit)
    }

    override suspend fun updateContactName(contactId: String, newName: String): Result<Unit> {
        updatedContactNames += contactId to newName
        return result(Unit)
    }

    override fun observeContactAliases(): Flow<Map<String, String>> = contactAliases

    override suspend fun refreshContactAliases(): Result<Unit> = result(Unit)
}
