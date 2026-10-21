package com.example.messenger.presentation.screens.changeaccount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ChangeAccountScreen(
    viewModel: ChangeAccountViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChangeAccountScreenContent(
        state = state,
        onBackClick = onBackClick,
        onSelect = viewModel::selectAccount,
    )
}
