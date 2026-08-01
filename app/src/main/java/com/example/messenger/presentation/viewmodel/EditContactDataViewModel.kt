package com.example.messenger.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.messenger.domain.repository.IUserRepository
import com.example.messenger.domain.usecase.user.GetUserByIdUseCase
import com.example.messenger.R
import com.example.messenger.presentation.base.StateViewModel
import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.base.toUiText
import com.example.messenger.presentation.effect.EditContactDataEffect
import com.example.messenger.presentation.state.EditContactDataUiState
import com.example.messenger.util.Resource
import com.example.messenger.util.resolveDisplayName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditContactDataViewModel @Inject constructor(
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val userRepository: IUserRepository,
    savedStateHandle: SavedStateHandle,
) : StateViewModel<EditContactDataUiState, EditContactDataEffect>(
    initialState = EditContactDataUiState(),
) {

    init {
        val contactId: String? = savedStateHandle["contactId"]
        if (!contactId.isNullOrBlank()) load(contactId)
    }

    private fun load(contactId: String) {
        viewModelScope.launch {
            val alias = userRepository.observeContactAliases().first()[contactId].orEmpty()
            setState { copy(contactId = contactId, name = alias, initialName = alias) }
        }
        viewModelScope.launch {
            getUserByIdUseCase(contactId).collect { resource ->
                if (resource is Resource.Success) {
                    val handle = resolveDisplayName(resource.data?.username, null)
                    setState { copy(contactId = contactId, username = handle) }
                }
            }
        }
    }

    fun onNameChange(value: String) = setState { copy(name = value) }
    fun showDeleteConfirm() = setState { copy(showDeleteConfirm = true) }
    fun dismissDeleteConfirm() = setState { copy(showDeleteConfirm = false) }

    fun save() {
        val state = currentState
        if (state.contactId.isBlank()) {
            setState { copy(error = UiText.StringResource(R.string.edit_contact_error_missing_id)) }
            return
        }
        if (state.name.isBlank()) {
            setState { copy(error = UiText.StringResource(R.string.edit_contact_error_name_empty)) }
            return
        }
        viewModelScope.launch {
            setState { copy(isSaving = true, error = null) }
            val result = userRepository.updateContactName(state.contactId, state.name)
            result.fold(
                onSuccess = {
                    setState { copy(isSaving = false, initialName = state.name) }
                    emitEffect(EditContactDataEffect.Saved)
                },
                onFailure = { e ->
                    setState {
                        copy(
                            isSaving = false,
                            error = e.message?.toUiText()
                                ?: UiText.StringResource(R.string.edit_contact_error_save_failed),
                        )
                    }
                },
            )
        }
    }

    fun delete() {
        setState { copy(showDeleteConfirm = false) }
        emitEffect(EditContactDataEffect.Deleted)
    }
}
