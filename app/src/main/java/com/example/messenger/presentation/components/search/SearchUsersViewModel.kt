package com.example.messenger.presentation.components.search

import androidx.lifecycle.viewModelScope
import com.example.messenger.R
import com.example.messenger.data.repository.RecentContactsRepository
import com.example.messenger.domain.model.User
import com.example.messenger.domain.usecase.conversation.CreateConversationUseCase
import com.example.messenger.domain.usecase.user.SearchUsersUseCase
import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.base.mvi.StateViewModel
import com.example.messenger.presentation.base.toUiText
import com.example.messenger.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchUsersViewModel @Inject constructor(
    private val searchUsersUseCase: SearchUsersUseCase,
    private val createConversationUseCase: CreateConversationUseCase,
    private val recentContactsRepository: RecentContactsRepository,
) : StateViewModel<SearchUsersUiState, SearchUsersEffect>(initialState = SearchUsersUiState()) {

    private var searchJob: Job? = null

    init {
        recentContactsRepository.observeRecentSearched()
            .onEach { recents -> setState { copy(recentSearched = recents) } }
            .launchIn(viewModelScope)
        recentContactsRepository.observeRecentOpened()
            .onEach { recents -> setState { copy(recentOpened = recents) } }
            .launchIn(viewModelScope)
    }

    fun searchUsers(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            setState { copy(users = emptyList(), isLoading = false, error = null) }
            return
        }
        searchJob = viewModelScope.launch {
            searchUsersUseCase(query).collect { resource ->
                when (resource) {
                    is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false, users = resource.data, error = null) }
                    }
                    is Resource.Error -> setState { copy(isLoading = false, error = resource.message.toUiText()) }
                    is Resource.Failure -> setState { copy(isLoading = false, error = resource.exception.message?.toUiText()) }
                }
            }
        }
    }

    fun createConversationWithUser(user: User) {
        viewModelScope.launch {
            recentContactsRepository.recordSearched(user)
            setState { copy(isCreatingConversation = true) }
            val result = createConversationUseCase(user.id)
            result.fold(
                onSuccess = { conversation ->
                    setState { copy(isCreatingConversation = false) }
                    emitEffect(SearchUsersEffect.ConversationCreated(conversation))
                },
                onFailure = { e ->
                    setState {
                        copy(
                            isCreatingConversation = false,
                            error = e.message?.toUiText()
                                ?: UiText.StringResource(R.string.search_error_create_conversation_failed),
                        )
                    }
                },
            )
        }
    }
}
