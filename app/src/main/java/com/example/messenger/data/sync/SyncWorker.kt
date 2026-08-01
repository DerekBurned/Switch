package com.example.messenger.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.messenger.domain.usecase.sync.SyncAllDataUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val parameters: WorkerParameters,
    private val syncAllDataUseCase: SyncAllDataUseCase
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val result = syncAllDataUseCase()

        return if (result.isSuccess) {
            Result.success()
        } else {
            
            Result.retry()
        }
    }
}