package com.example.messenger.domain.usecase.sync

import com.example.messenger.domain.repository.ISyncRepository
import javax.inject.Inject

class SyncAllDataUseCase @Inject constructor(
    private val syncRepository: ISyncRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return syncRepository.syncAllData()
    }
}