package com.example.messenger.domain.usecase.auth

import com.example.messenger.domain.model.User
import com.example.messenger.domain.repository.IAuthRepository
import com.example.messenger.util.Resource
import com.google.firebase.auth.PhoneAuthCredential
import javax.inject.Inject

class LoginWithPhoneNumberUseCase @Inject constructor(
    private val authRepository: IAuthRepository,
) {
    
    suspend operator fun invoke(
        credential: PhoneAuthCredential,
        username: String? = null,
        isRegistration: Boolean = false,
    ): Resource<User> = authRepository.loginWithPhone(credential, username, isRegistration)
}
