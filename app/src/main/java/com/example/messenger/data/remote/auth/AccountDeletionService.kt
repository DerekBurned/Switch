package com.example.messenger.data.remote.auth

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountDeletionService @Inject constructor(
    private val functions: FirebaseFunctions,
) {

    suspend fun deleteAccount(): Result<Unit> = runCatching {
        functions.getHttpsCallable("deleteAccount").call().await()
        Unit
    }.onFailure { Log.e(TAG, "deleteAccount call failed", it) }

    private companion object {
        const val TAG = "AccountDeletion"
    }
}
