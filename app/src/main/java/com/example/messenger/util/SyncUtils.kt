package com.example.messenger.util

sealed class SyncUtils<out T>{
    data class Syncing(val progress: Int): SyncUtils<Int>()
    data class Success<T>(val data: T): SyncUtils<T>()
    data class Error(val message: String): SyncUtils<Nothing>()
    inline fun <T,R> SyncUtils<T>.fold(
        onSyncing: (Int) -> R,
        onSuccess: (T) -> R,
        onError: (String) -> R
    ):R {
        return when (this){
            is SyncUtils.Syncing -> onSyncing(progress)
            is SyncUtils.Success -> onSuccess(data)
            is SyncUtils.Error -> onError(message) 
        }
    }
}