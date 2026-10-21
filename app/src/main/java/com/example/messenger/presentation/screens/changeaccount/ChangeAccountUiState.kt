package com.example.messenger.presentation.screens.changeaccount

import com.example.messenger.domain.model.User
import com.example.messenger.presentation.base.mvi.UiState

data class ChangeAccountUiState(
    val accounts: List<AccountSummary> = emptyList(),
    val currentAccountId: String = "",
    val isLoading: Boolean = false,
) : UiState

fun User.toAccountSummary() = AccountSummary(
    id = id,
    name = username.orEmpty(),
    phone = phoneNumber?.getFullNumber().orEmpty(),
)
