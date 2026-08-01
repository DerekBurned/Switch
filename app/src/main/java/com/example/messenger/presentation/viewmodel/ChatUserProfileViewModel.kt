package com.example.messenger.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.messenger.domain.repository.IUserRepository
import com.example.messenger.domain.usecase.user.GetUserByIdUseCase
import com.example.messenger.presentation.base.toUiText
import com.example.messenger.presentation.state.ChatUserProfileUiState
import com.example.messenger.presentation.state.MediaTab
import com.example.messenger.util.Resource
import com.example.messenger.util.resolveDisplayName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatUserProfileViewModel @Inject constructor(
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val userRepository: IUserRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUserProfileUiState())
    val uiState: StateFlow<ChatUserProfileUiState> = _uiState.asStateFlow()

    init {
        val userId: String? = savedStateHandle["userId"]
        val seededAvatarUrl: String? = savedStateHandle.get<String>("avatarUrl")?.takeIf { it.isNotBlank() }
        val seededName: String? = savedStateHandle.get<String>("partnerName")?.takeIf { it.isNotBlank() }
        if (seededAvatarUrl != null || seededName != null) {
            _uiState.update {
                it.copy(
                    seededAvatarUrl = seededAvatarUrl ?: it.seededAvatarUrl,
                    displayName = seededName ?: it.displayName,
                )
            }
        }
        if (!userId.isNullOrBlank()) {
            load(userId)
            loadPhotos(userId)
        }
    }

    private fun loadPhotos(userId: String) {
        viewModelScope.launch {
            userRepository.getProfilePhotos(userId).onSuccess { urls ->
                _uiState.update { it.copy(avatarPhotos = urls) }
            }
        }
    }

    private fun load(userId: String) {
        viewModelScope.launch {
            combine(
                getUserByIdUseCase(userId),
                userRepository.observeContactAliases(),
            ) { resource, aliases -> resource to aliases[userId] }
                .collect { (resource, alias) ->
                    when (resource) {
                        is Resource.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                        is Resource.Success -> _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = resource.data,
                                displayName = resolveDisplayName(resource.data?.username, alias),
                                isOnline = resource.data?.isOnline == true,
                            )
                        }
                        is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = resource.message.toUiText()) }
                        is Resource.Failure -> _uiState.update { it.copy(isLoading = false, error = resource.exception.message?.toUiText()) }
                    }
                }
        }
    }

    fun onMediaTabChange(tab: MediaTab) = _uiState.update { it.copy(mediaTab = tab) }
}
