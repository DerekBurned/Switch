package com.example.messenger.domain.usecase.user

import com.example.messenger.domain.model.User
import com.example.messenger.domain.repository.IUserRepository
import com.example.messenger.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetUserByIdUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    operator fun invoke(userId: String): Flow<Resource<User?>> = flow {
        try {
            emit(Resource.Loading)

            if (userId.isBlank()) {
                emit(Resource.Error("Invalid user ID"))
                return@flow
            }
            val result = userRepository.getUserById(userId)

            result.fold(
                onSuccess = { user ->
                    emit(Resource.Success(user))
                },
                onFailure = { exception ->
                    emit(Resource.Error(exception.message ?: "Unknown error"))
                }
            )

        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unexpected error occurred"))
        }
    }
}