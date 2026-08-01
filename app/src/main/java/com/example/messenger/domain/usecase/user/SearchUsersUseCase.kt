package com.example.messenger.domain.usecase.user

import androidx.compose.foundation.isSystemInDarkTheme
import com.example.messenger.domain.model.User
import com.example.messenger.domain.repository.IUserRepository
import com.example.messenger.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SearchUsersUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(query: String): Flow<Resource<List<User>>> = flow  {
        try {
            emit(Resource.Loading)
            val trimmedQuery = query.trim()
            if(trimmedQuery.isBlank()){
                emit(Resource.Success(emptyList()))
                return@flow
            }
            if(trimmedQuery.length  < 2){
                emit(Resource.Error("Query is too short"))
                return@flow
            }
            val result  = userRepository.searchUsers(trimmedQuery)
        result.fold(
            onSuccess = { userList ->
                emit(Resource.Success(userList))
            },
            onFailure = {exception ->
                emit(Resource.Error(exception.message?: "Search Failed"))
            }
        )

        }catch (e: Exception){
            emit(Resource.Error("Internal error occurred"))
        }

    }
}