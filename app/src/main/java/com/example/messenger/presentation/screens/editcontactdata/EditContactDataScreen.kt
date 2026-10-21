package com.example.messenger.presentation.screens.editcontactdata

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.presentation.base.ObserveAsEvents

@Composable
fun EditContactDataScreen(
    viewModel: EditContactDataViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.effect) { effect ->
        when (effect) {
            EditContactDataEffect.Saved, EditContactDataEffect.Deleted -> onBackClick()
        }
    }

    EditContactDataScreenContent(
        state = state,
        onBackClick = onBackClick,
        onNameChange = viewModel::onNameChange,
        onSave = viewModel::save,
        onShowDeleteConfirm = viewModel::showDeleteConfirm,
        onDismissDeleteConfirm = viewModel::dismissDeleteConfirm,
        onConfirmDelete = viewModel::delete,
    )
}
