package com.example.messenger.domain.usecase.auth

import com.example.messenger.domain.model.User
import com.example.messenger.domain.repository.IAuthRepository
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    operator fun invoke(): User? {
        return authRepository.getCurrentUser()
    }
}