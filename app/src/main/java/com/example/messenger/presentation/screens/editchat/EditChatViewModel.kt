package com.example.messenger.presentation.screens.editchat

import androidx.lifecycle.viewModelScope
import com.example.messenger.domain.usecase.conversation.DeleteConversationUseCase
import com.example.messenger.domain.usecase.conversation.MarkConversationAsReadUseCase
import com.example.messenger.domain.usecase.conversation.ObserveConversationsUseCase
import com.example.messenger.presentation.base.mvi.StateViewModel
import com.example.messenger.presentation.base.toUiText
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

    private var seeded = false

    init {
        viewModelScope.launch {
            observeConversationsUseCase()
                .onStart { setState { copy(isLoading = true) } }
                .catch { e -> setState { copy(isLoading = false, error = e.message?.toUiText()) } }
                .collect { list ->
                    val seedSelection = !seeded && list.isNotEmpty()
                    if (seedSelection) seeded = true
                    setState {
                        copy(
                            isLoading = false,
                            conversations = list,
                            selectedIds = if (seedSelection) list.map { c -> c.id }.toSet() else selectedIds,
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
