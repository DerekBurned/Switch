package com.example.messenger.data.repository

import android.util.Log
import com.example.messenger.data.crypto.E2eeBootstrap
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.data.remote.firebase.FirestoreService
import com.example.messenger.domain.model.DomainUser
import com.example.messenger.domain.model.User
import com.example.messenger.domain.repository.IAuthRepository
import com.example.messenger.util.Resource
import com.google.firebase.auth.PhoneAuthCredential
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuthService,
    private val firestore: FirestoreService,
    private val e2eeBootstrap: E2eeBootstrap,
) : IAuthRepository {

    override suspend fun loginWithPhone(
        credential: PhoneAuthCredential,
        username: String?,
        isRegistration: Boolean,
    ): Resource<User> {
        Log.d(TAG, "loginWithPhone: start username='$username' isRegistration=$isRegistration")
        return try {
            val firebaseUser = auth.signInWithPhone(credential).getOrThrow()
            Log.d(TAG, "loginWithPhone: signed in uid=${firebaseUser.uid}")
            val existing = firestore.getUserProfile(firebaseUser.uid).getOrNull()
            Log.d(TAG, "loginWithPhone: existing profile=${existing != null}")

            if (existing != null) {
                if (isRegistration) {
                    Log.w(TAG, "loginWithPhone: REGISTER for an already-registered number — rejecting")
                    auth.logout()
                    return Resource.Error("An account with this number already exists. Please log in instead.")
                }
                Log.d(TAG, "loginWithPhone: LOGIN existing user ${existing.id}")
                if (existing.usernameLower.isNullOrBlank() && !existing.username.isNullOrBlank()) {
                    runCatching {
                        firestore.updateUserProfile(
                            existing.id,
                            mapOf("usernameLower" to existing.username!!.lowercase()),
                        )
                    }
                }
                e2eeBootstrap.publishKeys()
                return Resource.Success(existing)
            }

            if (!isRegistration) {
                Log.w(TAG, "loginWithPhone: LOGIN for unregistered phone — rejecting and deleting orphan auth user")
                auth.deleteAccount()
                    .onFailure { Log.e(TAG, "loginWithPhone: failed to delete orphan auth user", it) }
                return Resource.Error("User does not exist. Please register first.")
            }

            val resolvedUsername = username?.trim()?.takeIf { it.isNotBlank() }
                ?: firebaseUser.displayName?.takeIf { it.isNotBlank() && !it.startsWith("+") }
            val user = User(
                id = firebaseUser.uid,
                username = resolvedUsername,
                usernameLower = resolvedUsername?.lowercase(),
                email = null,
                phoneNumber = null,
                avatarUrl = firebaseUser.photoUrl?.toString(),
                lastSeen = System.currentTimeMillis(),
                isOnline = true,
                fcmToken = null,
            ).also {
                Log.d(TAG, "loginWithPhone: REGISTER creating Firestore profile for ${it.id}")
                firestore.createUserProfile(it).getOrThrow()
                Log.d(TAG, "loginWithPhone: profile created")
                e2eeBootstrap.publishKeys()
            }
            Resource.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "loginWithPhone: FAILED", e)
            Resource.Error("${e.message}, Phone sign-in failed")
        }
    }

    override suspend fun linkPhoneToAccount(credential: PhoneAuthCredential): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading)
            auth.linkPhone(credential).getOrThrow()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("${e.message}, Internal error occurred"))
        }
    }

    override suspend fun logout() {
        return auth.logout()
    }

    override fun getCurrentUser(): User? {
        val firebaseUser = auth.getCurrentUser()
        return firebaseUser?.let {
            User(
                id = it.uid,
                username = firebaseUser.displayName,
                email = null,
                phoneNumber = null,
                avatarUrl = firebaseUser.photoUrl?.toString(),
                lastSeen = System.currentTimeMillis(),
                isOnline = false,
                fcmToken = null,
            )
        }
    }

    override fun observeAuthState(): Flow<DomainUser?> {
        return auth.observeAuthState().map { firebaseUser ->
            firebaseUser?.let {
                DomainUser(
                    id = it.uid,
                    displayName = it.displayName,
                    phoneNumber = it.phoneNumber,
                    photoUrl = it.photoUrl?.toString(),
                )
            }
        }
    }

    override suspend fun updateUserProfile(
        username: String,
        avatarUrl: String?,
    ): Result<Unit> {
        return auth.updateProfile(username, avatarUrl)
    }

    private companion object {
        const val TAG = "AUTHFLOW_REPO"
    }
}
