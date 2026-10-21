package com.example.messenger.domain.model

sealed interface SessionState {

    data object Resolving : SessionState

    data class SignedOut(val reason: SessionEndReason? = null) : SessionState

    data class Active(val user: User) : SessionState
}

enum class SessionEndReason {
    PROFILE_MISSING,
    ACCOUNT_DELETED,
}
