package com.example.messenger.presentation.screens.forwardmessage

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardMessageBottomSheet(
    onDismiss: () -> Unit,
    onForward: (conversationId: String) -> Unit,
    viewModel: ForwardMessageViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    ForwardMessageBottomSheetContent(
        state = state,
        currentUserId = currentUserId,
        onDismiss = onDismiss,
        onQueryChange = viewModel::onQueryChange,
        onSearchActiveChange = viewModel::setSearchActive,
        onForward = { id ->
            onForward(id)
            onDismiss()
        },
    )
}
