package com.example.messenger.domain.model

sealed class AuthAccountException(message: String) : Exception(message) {
    class AlreadyRegistered : AuthAccountException("An account with this number already exists")
    class NotRegistered : AuthAccountException("No account uses this number")
}
