package com.example.messenger.util

sealed class Resource<out T> {
    data class Success<T>(val data:T): Resource<T>()
    data class Error(val message:String): Resource<Nothing>()
    data class Failure(val exception: Exception): Resource<Nothing>()
    object Loading: Resource<Nothing>()

    inline fun <T, R> Resource<T>.fold(
        onSuccess: (T) -> R,
        onError: (String) -> R,
        onFailure: (Exception) -> R,
        onLoading: () -> R
    ): R {
        return when (this) {
            is Resource.Success -> onSuccess(data)
            is Resource.Error -> onError(message)
            is Resource.Failure -> onFailure(exception)
            is Resource.Loading -> onLoading()
        }
    }
}