package com.example.messenger.domain.usecase.auth

import com.example.messenger.domain.repository.IAuthRepository
import com.example.messenger.domain.repository.ILocalRepository
import com.example.messenger.domain.service.ISessionCleaner
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val authRepository: IAuthRepository,
    private val localRepository: ILocalRepository,
    private val sessionCleaner: ISessionCleaner,
) {

    suspend operator fun invoke(): Result<Unit> {
        val uid = authRepository.getCurrentUser()?.id

        val deletion = authRepository.deleteAccount()
        if (deletion.isFailure) return deletion

        sessionCleaner.clearOnDelete(uid)
        authRepository.logout()
        localRepository.resetDB()
        return Result.success(Unit)
    }
}
