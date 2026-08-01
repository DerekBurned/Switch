package com.example.messenger.presentation.state

import com.example.messenger.domain.model.User
import com.example.messenger.presentation.base.UiState

data class AccountSummary(
    val id: String,
    val name: String,
    val phone: String,
)

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
