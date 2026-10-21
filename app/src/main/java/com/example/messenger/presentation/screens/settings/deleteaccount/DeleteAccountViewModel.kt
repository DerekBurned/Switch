package com.example.messenger.presentation.screens.settings.deleteaccount

import android.app.Activity
import androidx.lifecycle.viewModelScope
import com.example.messenger.R
import com.example.messenger.data.presence.PresenceManager
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.domain.usecase.auth.DeleteAccountUseCase
import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.base.mvi.MviViewModel
import com.example.messenger.presentation.base.toPhoneAuthUiText
import com.example.messenger.util.VerificationResult
import com.google.firebase.auth.PhoneAuthCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val firebaseAuthService: FirebaseAuthService,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val presenceManager: PresenceManager,
) : MviViewModel<DeleteAccountUiState, DeleteAccountIntent, DeleteAccountEffect>(
    initialState = DeleteAccountUiState(phone = firebaseAuthService.getUserPhoneNumber().orEmpty()),
) {

    override fun handleIntent(intent: DeleteAccountIntent) {
        when (intent) {
            is DeleteAccountIntent.ConfirmationChange ->
                setState { copy(confirmation = intent.value, error = null) }

            is DeleteAccountIntent.OtpChange -> setState { copy(otp = intent.value) }

            is DeleteAccountIntent.SendVerificationCode -> sendVerificationCode(intent.activity)

            DeleteAccountIntent.VerifyOtpAndDelete -> verifyOtpAndDelete()

            DeleteAccountIntent.ClearError -> setState { copy(error = null) }

            DeleteAccountIntent.GoBack -> setState {
                copy(step = DeleteAccountStep.CONFIRM, otp = "", error = null, isLoading = false)
            }
        }
    }

    private fun sendVerificationCode(activity: Activity) {
        val state = currentState
        if (!state.isConfirmed) {
            setState { copy(error = UiText.StringResource(R.string.delete_account_error_confirm)) }
            return
        }
        if (state.phone.isBlank()) {
            setState { copy(error = UiText.StringResource(R.string.delete_account_error_no_number)) }
            return
        }

        setState { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            firebaseAuthService.sendVerificationCode(state.phone, activity).fold(
                onSuccess = { verification ->
                    when (verification) {
                        is VerificationResult.CodeSent -> setState {
                            copy(
                                step = DeleteAccountStep.VERIFY,
                                otp = "",
                                verificationId = verification.verificationId,
                                isLoading = false,
                            )
                        }

                        is VerificationResult.AutoVerified -> reauthenticateAndDelete(verification.credential)
                    }
                },
                onFailure = { e -> fail(e.toPhoneAuthUiText()) },
            )
        }
    }

    private fun verifyOtpAndDelete() {
        setState { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            firebaseAuthService.verifyCode(currentState.otp, currentState.verificationId).fold(
                onSuccess = { reauthenticateAndDelete(it) },
                onFailure = { e ->
                    fail(e.toPhoneAuthUiText(R.string.auth_error_invalid_code))
                },
            )
        }
    }

    private suspend fun reauthenticateAndDelete(credential: PhoneAuthCredential) {
        val reauth = firebaseAuthService.reauthenticateWithPhone(credential)
        if (reauth.isFailure) {
            fail(
                reauth.exceptionOrNull()?.toPhoneAuthUiText(R.string.delete_account_error_reauth)
                    ?: UiText.StringResource(R.string.delete_account_error_reauth),
            )
            return
        }

        presenceManager.disconnect(viewModelScope)
        deleteAccountUseCase().fold(
            onSuccess = {
                setState { copy(isLoading = false) }
                emitEffect(DeleteAccountEffect.Deleted)
            },
            onFailure = { e ->
                fail(
                    e.message?.let { UiText.DynamicString(it) }
                        ?: UiText.StringResource(R.string.delete_account_error_failed),
                )
            },
        )
    }

    private fun fail(message: UiText) {
        setState { copy(isLoading = false, error = message) }
    }
}
