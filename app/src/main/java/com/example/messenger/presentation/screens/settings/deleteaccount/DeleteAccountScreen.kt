package com.example.messenger.presentation.screens.settings.deleteaccount

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.presentation.base.ObserveAsEvents

@Composable
fun DeleteAccountScreen(
    viewModel: DeleteAccountViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onDeleted: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    ObserveAsEvents(viewModel.effect) { effect ->
        when (effect) {
            DeleteAccountEffect.Deleted -> onDeleted()
        }
    }

    DeleteAccountScreenContent(
        state = state,
        onBack = {
            if (state.step == DeleteAccountStep.CONFIRM) {
                onBackClick()
            } else {
                viewModel.dispatch(DeleteAccountIntent.GoBack)
            }
        },
        onConfirmationChange = { viewModel.dispatch(DeleteAccountIntent.ConfirmationChange(it)) },
        onOtpChange = { viewModel.dispatch(DeleteAccountIntent.OtpChange(it)) },
        onStartDeletion = {
            activity?.let { viewModel.dispatch(DeleteAccountIntent.SendVerificationCode(it)) }
        },
        onVerify = { viewModel.dispatch(DeleteAccountIntent.VerifyOtpAndDelete) },
    )
}
