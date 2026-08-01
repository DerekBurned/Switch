package com.example.messenger.testutil.fakes

import com.example.messenger.domain.model.DomainUser
import com.example.messenger.domain.model.User
import com.example.messenger.domain.repository.IAuthRepository
import com.example.messenger.util.Resource
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeAuthRepository : IAuthRepository {

    var loggedInUser: User? = null
    val authState = MutableStateFlow<DomainUser?>(null)
    var loginResult: Resource<User> = Resource.Success(User())
    var loggedOut = false

    override suspend fun linkPhoneToAccount(credential: PhoneAuthCredential): Flow<Resource<Unit>> =
        flowOf(Resource.Success(Unit))

    override suspend fun loginWithPhone(
        credential: PhoneAuthCredential,
        username: String?,
        isRegistration: Boolean,
    ): Resource<User> = loginResult

    override suspend fun logout() {
        loggedOut = true
        loggedInUser = null
    }

    override fun getCurrentUser(): User? = loggedInUser

    override fun observeAuthState(): Flow<DomainUser?> = authState

    override suspend fun updateUserProfile(username: String, avatarUrl: String?): Result<Unit> =
        Result.success(Unit)
}
