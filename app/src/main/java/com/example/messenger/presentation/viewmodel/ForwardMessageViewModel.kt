package com.example.messenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.messenger.domain.usecase.conversation.GetConversationsUseCase
import com.example.messenger.presentation.base.toUiText
import com.example.messenger.presentation.state.ForwardMessageUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForwardMessageViewModel @Inject constructor(
    private val getConversationsUseCase: GetConversationsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForwardMessageUiState())
    val uiState: StateFlow<ForwardMessageUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getConversationsUseCase()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message?.toUiText()) } }
                .collect { list ->
                    _uiState.update { it.copy(isLoading = false, conversations = list, error = null) }
                }
        }
    }

    fun onQueryChange(q: String) = _uiState.update { it.copy(query = q) }
    fun setSearchActive(active: Boolean) = _uiState.update {
        it.copy(searchActive = active, query = if (active) it.query else "")
    }
}
