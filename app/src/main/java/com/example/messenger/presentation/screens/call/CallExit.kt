package com.example.messenger.presentation.screens.call

sealed interface CallExit {
    data object Pop : CallExit
    data object Unavailable : CallExit
    data class OpenChat(
        val conversationId: String,
        val partnerId: String,
        val partnerName: String,
    ) : CallExit
}
