package com.example.messenger.domain.repository

import com.example.messenger.domain.model.DomainUser
import com.example.messenger.domain.model.User
import com.example.messenger.util.Resource
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    suspend fun linkPhoneToAccount(credential: PhoneAuthCredential): Flow<Resource<Unit>>

    suspend fun loginWithPhone(
        credential: PhoneAuthCredential,
        username: String? = null,
        isRegistration: Boolean = false,
    ): Resource<User>

    suspend fun logout()
    fun getCurrentUser(): User?
    fun observeAuthState(): Flow<DomainUser?>
    suspend fun updateUserProfile(username: String, avatarUrl: String?): Result<Unit>
}
