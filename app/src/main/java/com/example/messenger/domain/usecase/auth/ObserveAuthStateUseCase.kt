package com.example.messenger.domain.usecase.auth

import com.example.messenger.domain.model.DomainUser
import com.example.messenger.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAuthStateUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    operator fun invoke(): Flow<DomainUser?> {
        return authRepository.observeAuthState()
    }
}
