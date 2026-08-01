package com.example.messenger.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messenger.domain.usecase.conversation.DeleteConversationUseCase
import com.example.messenger.domain.usecase.conversation.MarkConversationAsReadUseCase
import com.example.messenger.domain.usecase.conversation.ObserveConversationsUseCase
import com.example.messenger.presentation.base.StateViewModel
import com.example.messenger.presentation.base.toUiText
import com.example.messenger.presentation.effect.EditChatEffect
import com.example.messenger.presentation.state.EditChatUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditChatViewModel @Inject constructor(
    private val observeConversationsUseCase: ObserveConversationsUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
    private val markConversationAsReadUseCase: MarkConversationAsReadUseCase,
) : StateViewModel<EditChatUiState, EditChatEffect>(initialState = EditChatUiState()) {

    init {
        viewModelScope.launch {
            observeConversationsUseCase()
                .onStart { setState { copy(isLoading = true) } }
                .catch { e -> setState { copy(isLoading = false, error = e.message?.toUiText()) } }
                .collect { list ->
                    setState {
                        copy(
                            isLoading = false,
                            conversations = list,
                            
                            selectedIds = if (selectedIds.isEmpty()) list.map { c -> c.id }.toSet() else selectedIds,
                        )
                    }
                }
        }
    }

    fun toggle(id: String) {
        setState {
            val next = selectedIds.toMutableSet().apply {
                if (contains(id)) remove(id) else add(id)
            }
            copy(selectedIds = next)
        }
    }

    fun deleteSelected() {
        val ids = currentState.selectedIds.toList()
        viewModelScope.launch {
            ids.forEach { deleteConversationUseCase(it) }
            emitEffect(EditChatEffect.Done)
        }
    }

    fun markAllRead() {
        val ids = currentState.selectedIds.toList()
        viewModelScope.launch {
            ids.forEach { markConversationAsReadUseCase(it) }
            emitEffect(EditChatEffect.Done)
        }
    }
}
