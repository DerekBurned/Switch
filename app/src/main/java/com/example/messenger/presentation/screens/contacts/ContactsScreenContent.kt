package com.example.messenger.presentation.screens.contacts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.domain.model.User
import com.example.messenger.presentation.components.search.SearchUsersViewModel

@Composable
fun ContactsScreenContent(
    modifier: Modifier = Modifier,
    viewModel: SearchUsersViewModel = hiltViewModel(),
    onContactClick: (User) -> Unit = {},
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    ContactsScreenContentInner(
        modifier = modifier,
        users = uiState.users,
        isLoading = uiState.isLoading,
        query = query,
        onQueryChange = {
            query = it
            viewModel.searchUsers(it)
        },
        onContactClick = onContactClick,
    )
}
