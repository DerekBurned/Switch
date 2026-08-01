package com.example.messenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.messenger.domain.usecase.auth.GetCurrentUserUseCase
import com.example.messenger.presentation.state.ChangeAccountUiState
import com.example.messenger.presentation.state.toAccountSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ChangeAccountViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChangeAccountUiState())
    val uiState: StateFlow<ChangeAccountUiState> = _uiState.asStateFlow()

    init {
        val user = getCurrentUserUseCase()
        if (user != null) {
            val account = user.toAccountSummary()
            _uiState.update { it.copy(accounts = listOf(account), currentAccountId = account.id) }
        }
    }

    fun selectAccount(id: String) = _uiState.update { it.copy(currentAccountId = id) }
}
