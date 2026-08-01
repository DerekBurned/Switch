package com.example.messenger.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.domain.repository.IUserRepository
import com.example.messenger.R
import com.example.messenger.presentation.base.MviViewModel
import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.base.toUiText
import com.example.messenger.presentation.effect.EditProfileEffect
import com.example.messenger.presentation.intent.EditProfileIntent
import com.example.messenger.presentation.state.EditProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: IUserRepository,
    private val firebaseAuthService: FirebaseAuthService,
    private val savedStateHandle: SavedStateHandle,
) : MviViewModel<EditProfileUiState, EditProfileIntent, EditProfileEffect>(
    initialState = EditProfileUiState(
        name = savedStateHandle[KEY_NAME] ?: "",
        username = savedStateHandle[KEY_USERNAME] ?: "",
        phone = savedStateHandle[KEY_PHONE] ?: "",
        dob = savedStateHandle[KEY_DOB] ?: "",
    ),
) {

    init {

        if (savedStateHandle.get<Boolean>(KEY_LOADED) != true) {
            loadProfile()
        }
    }

    override fun handleIntent(intent: EditProfileIntent) {
        when (intent) {
            is EditProfileIntent.NameChange -> {
                savedStateHandle[KEY_NAME] = intent.value
                setState { copy(name = intent.value) }
            }
            is EditProfileIntent.PhoneChange -> {
                savedStateHandle[KEY_PHONE] = intent.value
                setState { copy(phone = intent.value) }
            }
            is EditProfileIntent.UsernameChange -> {
                savedStateHandle[KEY_USERNAME] = intent.value
                setState { copy(username = intent.value) }
            }
            is EditProfileIntent.DobChange -> {
                savedStateHandle[KEY_DOB] = intent.value
                setState { copy(dob = intent.value) }
            }
            EditProfileIntent.Save -> save()
            EditProfileIntent.ClearError -> setState { copy(error = null) }
        }
    }

    private fun loadProfile() {
        val uid = firebaseAuthService.getCurrentUserId() ?: return
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            val result = userRepository.getUserById(uid)
            val user = result.getOrNull()
            val phoneFromAuth = firebaseAuthService.getUserPhoneNumber().orEmpty()
            val phoneFromProfile = user?.phoneNumber?.getFullNumber().orEmpty()
            val name = user?.username.orEmpty()
            val username = user?.username.orEmpty()
            val phone = phoneFromProfile.ifBlank { phoneFromAuth }

            savedStateHandle[KEY_NAME] = name
            savedStateHandle[KEY_USERNAME] = username
            savedStateHandle[KEY_PHONE] = phone
            savedStateHandle[KEY_LOADED] = true

            setState {
                copy(
                    isLoading = false,
                    name = name,
                    username = username,
                    phone = phone,
                    avatarUrl = user?.avatarUrl,
                )
            }
        }
    }

    private fun save() {
        val state = currentState
        if (state.username.isBlank()) {
            setState { copy(error = UiText.StringResource(R.string.edit_profile_error_username_empty)) }
            return
        }
        viewModelScope.launch {
            setState { copy(isSaving = true, error = null) }
            val updates = mutableMapOf<String, Any>(
                "username" to state.username,
                "usernameLower" to state.username.lowercase(),
            )
            val result = userRepository.updateUserProfile(updates)
            result.fold(
                onSuccess = {
                    
                    firebaseAuthService.updateProfile(displayName = state.username, photoUrl = null)
                    savedStateHandle[KEY_NAME] = state.username
                    setState { copy(isSaving = false, name = state.username) }
                    emitEffect(EditProfileEffect.Saved)
                },
                onFailure = { e ->
                    setState {
                        copy(
                            isSaving = false,
                            error = e.message?.toUiText()
                                ?: UiText.StringResource(R.string.edit_profile_error_save_failed),
                        )
                    }
                },
            )
        }
    }

    private companion object {
        const val KEY_LOADED = "edit_profile_loaded"
        const val KEY_NAME = "edit_profile_name"
        const val KEY_USERNAME = "edit_profile_username"
        const val KEY_PHONE = "edit_profile_phone"
        const val KEY_DOB = "edit_profile_dob"
    }
}
