package com.example.messenger.domain.repository

import android.net.Uri
import com.example.messenger.domain.model.User
import kotlinx.coroutines.flow.Flow

interface IUserRepository {
    suspend fun getUserById(userId: String): Result<User?>
    suspend fun searchUsers(query: String): Result<List<User>>
    suspend fun uploadAvatar(imageUri: Uri): Result<String>
    suspend fun getProfilePhotos(userId: String): Result<List<String>>
    fun observeUser(userId: String): Flow<User?>
    suspend fun updateUserStatus(isOnline: Boolean): Result<Unit>
    suspend fun updateLastSeen(): Result<Unit>
    suspend fun updateUserProfile(updates: Map<String, Any>): Result<Unit>
    suspend fun updateContactName(contactId: String, newName: String): Result<Unit>
    fun observeContactAliases(): Flow<Map<String, String>>
    suspend fun refreshContactAliases(): Result<Unit>
}