package com.example.messenger.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.domain.model.PhoneNumber
import com.example.messenger.domain.repository.IUserRepository
import com.example.messenger.R
import com.example.messenger.presentation.base.MviViewModel
import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.base.toUiText
import com.example.messenger.presentation.components.auth.Countries
import com.example.messenger.presentation.effect.ChangePhoneEffect
import com.example.messenger.presentation.intent.ChangePhoneIntent
import com.example.messenger.presentation.state.ChangePhoneStep
import com.example.messenger.presentation.state.ChangePhoneUiState
import com.example.messenger.util.ValidationUtils
import com.example.messenger.util.VerificationResult
import com.google.firebase.auth.PhoneAuthCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangePhoneViewModel @Inject constructor(
    private val firebaseAuthService: FirebaseAuthService,
    private val userRepository: IUserRepository,
    private val savedStateHandle: SavedStateHandle,
) : MviViewModel<ChangePhoneUiState, ChangePhoneIntent, ChangePhoneEffect>(
    initialState = restoreInitialState(savedStateHandle, firebaseAuthService),
) {

    private companion object {
        const val KEY_STEP = "change_phone_step"
        const val KEY_CURRENT_PHONE = "change_phone_current"
        const val KEY_NEW_ISO = "change_phone_new_iso"
        const val KEY_NEW_NUMBER = "change_phone_new_number"
        const val KEY_REAUTH_AT = "change_phone_reauth_at"

        const val REAUTH_VALIDITY_MS = 3 * 60 * 1000L

        fun restoreInitialState(
            handle: SavedStateHandle,
            auth: FirebaseAuthService,
        ): ChangePhoneUiState {
            val persistedStep = handle.get<String>(KEY_STEP)
                ?.let { runCatching { ChangePhoneStep.valueOf(it) }.getOrNull() }
                ?: ChangePhoneStep.CONFIRM_CURRENT
            val persistedCurrent = handle.get<String>(KEY_CURRENT_PHONE).orEmpty()
            val persistedIso = handle.get<String>(KEY_NEW_ISO).orEmpty()
            val persistedNumber = handle.get<String>(KEY_NEW_NUMBER).orEmpty()
            val country = Countries.all.firstOrNull { it.isoCode == persistedIso }
                ?: Countries.default

            val currentPhone = persistedCurrent.ifBlank {
                auth.getUserPhoneNumber().orEmpty()
            }
            handle[KEY_CURRENT_PHONE] = currentPhone

            return ChangePhoneUiState(
                step = persistedStep,
                currentPhone = currentPhone,
                newCountry = country,
                newNationalNumber = persistedNumber,
            )
        }
    }

    private var reauthSucceededAt: Long
        get() = savedStateHandle[KEY_REAUTH_AT] ?: 0L
        set(value) { savedStateHandle[KEY_REAUTH_AT] = value }

    override fun handleIntent(intent: ChangePhoneIntent) {
        when (intent) {
            is ChangePhoneIntent.NewCountrySelected -> {
                savedStateHandle[KEY_NEW_ISO] = intent.country.isoCode
                setState { copy(newCountry = intent.country) }
            }
            is ChangePhoneIntent.NewNumberChange -> {
                val digits = intent.value.filter { ch -> ch.isDigit() }
                savedStateHandle[KEY_NEW_NUMBER] = digits
                setState { copy(newNationalNumber = digits) }
            }
            is ChangePhoneIntent.OtpChange -> setState { copy(otp = intent.value) }
            ChangePhoneIntent.ClearError -> setState { copy(error = null) }
            is ChangePhoneIntent.SendCodeToCurrentPhone -> sendCodeToCurrentPhone(intent.activity)
            ChangePhoneIntent.VerifyCurrentOtp -> verifyCurrentOtp()
            is ChangePhoneIntent.SendCodeToNewPhone -> sendCodeToNewPhone(intent.activity)
            ChangePhoneIntent.VerifyNewOtp -> verifyNewOtp()
            ChangePhoneIntent.GoBack -> goBack()
        }
    }

    private fun sendCodeToCurrentPhone(activity: Activity) {
        val phone = currentState.currentPhone
        if (phone.isBlank()) {
            setState { copy(error = UiText.StringResource(R.string.change_phone_error_no_number_on_file)) }
            return
        }
        sendOtp(activity, phone, onSent = {
            updateStep(ChangePhoneStep.VERIFY_CURRENT)
            setState { copy(otp = "", isLoading = false) }
        }, onAutoVerified = { credential ->
            reauthenticateAndAdvance(credential)
        })
    }

    private fun verifyCurrentOtp() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            val credentialResult = firebaseAuthService.verifyCode(currentState.otp)
            credentialResult.fold(
                onSuccess = { reauthenticateAndAdvance(it) },
                onFailure = { e ->
                    setState {
                        copy(
                            isLoading = false,
                            error = e.message?.toUiText()
                                ?: UiText.StringResource(R.string.change_phone_error_invalid_code),
                        )
                    }
                },
            )
        }
    }

    private fun reauthenticateAndAdvance(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            val result = firebaseAuthService.reauthenticateWithPhone(credential)
            result.fold(
                onSuccess = {
                    reauthSucceededAt = System.currentTimeMillis()
                    updateStep(ChangePhoneStep.ENTER_NEW)
                    setState { copy(otp = "", isLoading = false, error = null) }
                },
                onFailure = { e ->
                    setState {
                        copy(
                            isLoading = false,
                            error = e.message?.toUiText()
                                ?: UiText.StringResource(R.string.change_phone_error_could_not_verify_current),
                        )
                    }
                },
            )
        }
    }

    private fun sendCodeToNewPhone(activity: Activity) {
        val s = currentState
        val fullNewPhone = s.newCountry.dialCode + s.newNationalNumber
        if (s.newNationalNumber.isBlank()) {
            setState { copy(error = UiText.StringResource(R.string.change_phone_error_enter_new_number)) }
            return
        }
        if (!ValidationUtils.isValidPhoneNumber(fullNewPhone)) {
            setState { copy(error = UiText.StringResource(R.string.change_phone_error_invalid_number)) }
            return
        }
        if (!hasFreshReauth()) {
            failWithStaleReauth()
            return
        }
        sendOtp(activity, fullNewPhone, onSent = {
            updateStep(ChangePhoneStep.VERIFY_NEW)
            setState { copy(otp = "", isLoading = false) }
        }, onAutoVerified = { credential ->
            updatePhoneAndFinish(credential)
        })
    }

    private fun verifyNewOtp() {
        if (!hasFreshReauth()) {
            failWithStaleReauth()
            return
        }
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            val credentialResult = firebaseAuthService.verifyCode(currentState.otp)
            credentialResult.fold(
                onSuccess = { updatePhoneAndFinish(it) },
                onFailure = { e ->
                    setState {
                        copy(
                            isLoading = false,
                            error = e.message?.toUiText()
                                ?: UiText.StringResource(R.string.change_phone_error_invalid_code),
                        )
                    }
                },
            )
        }
    }

    private fun updatePhoneAndFinish(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            val updateResult = firebaseAuthService.updatePhoneNumber(credential)
            if (updateResult.isFailure) {
                setState {
                    copy(
                        isLoading = false,
                        error = updateResult.exceptionOrNull()?.message?.toUiText()
                            ?: UiText.StringResource(R.string.change_phone_error_update_failed),
                    )
                }
                return@launch
            }
            val s = currentState
            val newPhoneNumber = PhoneNumber(
                countryCode = s.newCountry.dialCode,
                number = s.newNationalNumber,
            )
            userRepository.updateUserProfile(mapOf("phoneNumber" to newPhoneNumber))
            clearPersistedFormState()
            setState { copy(isLoading = false) }
            emitEffect(ChangePhoneEffect.Done)
        }
    }

    private fun goBack() {
        val current = currentState.step
        val previous = when (current) {
            ChangePhoneStep.VERIFY_CURRENT -> ChangePhoneStep.CONFIRM_CURRENT
            ChangePhoneStep.ENTER_NEW -> ChangePhoneStep.CONFIRM_CURRENT
            ChangePhoneStep.VERIFY_NEW -> ChangePhoneStep.ENTER_NEW
            ChangePhoneStep.CONFIRM_CURRENT -> current
        }
        updateStep(previous)
        setState { copy(otp = "", error = null, isLoading = false) }
    }

    private fun hasFreshReauth(): Boolean {
        val at = reauthSucceededAt
        if (at == 0L) return false
        return System.currentTimeMillis() - at <= REAUTH_VALIDITY_MS
    }

    private fun failWithStaleReauth() {
        reauthSucceededAt = 0L
        updateStep(ChangePhoneStep.CONFIRM_CURRENT)
        setState {
            copy(
                otp = "",
                isLoading = false,
                error = UiText.StringResource(R.string.change_phone_error_reauth_expired),
            )
        }
    }

    private fun updateStep(step: ChangePhoneStep) {
        savedStateHandle[KEY_STEP] = step.name
        setState { copy(step = step) }
    }

    private fun clearPersistedFormState() {
        savedStateHandle[KEY_NEW_NUMBER] = ""
        savedStateHandle[KEY_NEW_ISO] = ""
        savedStateHandle[KEY_REAUTH_AT] = 0L
    }

    private fun sendOtp(
        activity: Activity,
        phoneNumber: String,
        onSent: () -> Unit,
        onAutoVerified: (PhoneAuthCredential) -> Unit,
    ) {
        setState { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = firebaseAuthService.sendVerificationCode(phoneNumber, activity)
            result.fold(
                onSuccess = { verification ->
                    when (verification) {
                        is VerificationResult.CodeSent -> onSent()
                        is VerificationResult.AutoVerified -> onAutoVerified(verification.credential)
                    }
                },
                onFailure = { e ->
                    setState {
                        copy(
                            isLoading = false,
                            error = e.message?.toUiText()
                                ?: UiText.StringResource(R.string.change_phone_error_verification_failed),
                        )
                    }
                },
            )
        }
    }
}
