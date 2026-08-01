package com.example.messenger.domain.repository

import com.example.messenger.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface ISyncRepository {
    suspend fun syncAllData(): Result<Unit>
    suspend fun processSyncQueue(): Result<Unit>
    fun observeSyncStatus(): Flow<SyncStatus>
    suspend fun getSyncQueueCount(): Int
}