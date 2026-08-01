package com.example.messenger.util

sealed class NetworkUtils<T> {
    object Available: NetworkUtils<Nothing>()
    object UnAvailable: NetworkUtils<Nothing>()
    object Losing: NetworkUtils<Nothing>()
    object Lost: NetworkUtils<Nothing>()
    data class Error(val message: String): NetworkUtils<Nothing>()
    inline fun <T, R> NetworkUtils<T>.fold(
        onAvailable: () -> R,
        onUnAvailable: () -> R,
        onLosing: () -> R,
        onLost: () -> R,
        onError: (String) -> R
    ): R {
        return when (this) {
            is NetworkUtils.Available -> onAvailable()
            is NetworkUtils.UnAvailable -> onUnAvailable()
            is NetworkUtils.Losing -> onLosing()
            is NetworkUtils.Lost -> onLost()
            is NetworkUtils.Error -> onError(message)
        }
    }
}