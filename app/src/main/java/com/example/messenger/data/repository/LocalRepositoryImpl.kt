package com.example.messenger.data.repository

import com.example.messenger.domain.repository.ILocalRepository
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
