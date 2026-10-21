package com.example.messenger.presentation.screens.editprofile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.presentation.base.ObserveAsEvents

@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onSaved: () -> Unit = {},
    onLogout: () -> Unit = {},
    onChangeAccount: () -> Unit = {},
    onChangePhone: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.effect) { effect ->
        when (effect) {
            EditProfileEffect.Saved -> onSaved()
        }
    }

    EditProfileScreenContent(
        state = state,
        onBackClick = onBackClick,
        onNameChange = { viewModel.dispatch(EditProfileIntent.NameChange(it)) },
        onUsernameChange = { viewModel.dispatch(EditProfileIntent.UsernameChange(it)) },
        onDobChange = { viewModel.dispatch(EditProfileIntent.DobChange(it)) },
        onSaveClick = { viewModel.dispatch(EditProfileIntent.Save) },
        onLogoutClick = onLogout,
        onChangeAccountClick = onChangeAccount,
        onChangePhoneClick = onChangePhone,
    )
}
