package com.example.messenger.domain.usecase.auth

import com.example.messenger.domain.repository.IAuthRepository
import com.example.messenger.domain.repository.ILocalRepository
import com.example.messenger.domain.repository.IUserRepository
import com.example.messenger.domain.service.ISessionCleaner
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: IAuthRepository,
    private val userRepository: IUserRepository,
    private val localRepository: ILocalRepository,
    private val sessionCleaner: ISessionCleaner,
) {

    suspend operator fun invoke() {
        runCatching { userRepository.updateUserStatus(isOnline = false) }
        val uid = authRepository.getCurrentUser()?.id
        sessionCleaner.clearOnLogout(uid)
        authRepository.logout()
        localRepository.resetDB()
    }
}
