package com.example.messenger.domain.usecase.auth

import com.example.messenger.domain.repository.IAuthRepository
import com.example.messenger.util.Resource
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LinkPhoneUseCase @Inject constructor(
    private val auth: IAuthRepository
) {
    suspend operator fun invoke(credential: PhoneAuthCredential): Flow<Resource<Unit>> {
        return auth.linkPhoneToAccount(credential)
    }
}