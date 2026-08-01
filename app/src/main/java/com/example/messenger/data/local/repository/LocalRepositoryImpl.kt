package com.example.messenger.data.local.repository

import io.objectbox.BoxStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalRepositoryImpl @Inject constructor(
    private val boxStore: BoxStore,
) : ILocalRepository {

    override suspend fun resetDB() = withContext(Dispatchers.IO) {
        boxStore.removeAllObjects()
    }
}
