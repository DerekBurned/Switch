package com.example.messenger.presentation.screens.auth

import android.app.Activity
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.messenger.R
import com.example.messenger.data.presence.PresenceManager
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.data.remote.firebase.FirebaseMessagingManager
import com.example.messenger.data.remote.firebase.FirestoreService
import com.example.messenger.data.sync.SyncCoordinator
import com.example.messenger.domain.model.SessionEndReason
import com.example.messenger.domain.model.SessionState
import com.example.messenger.domain.usecase.auth.LinkPhoneUseCase
import com.example.messenger.domain.usecase.auth.LoginWithPhoneNumberUseCase
import com.example.messenger.domain.usecase.auth.LogoutUseCase
import com.example.messenger.domain.usecase.auth.ObserveSessionStateUseCase
import com.example.messenger.domain.usecase.sync.BackfillRecentHistoryUseCase
import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.base.mvi.MviViewModel
import com.example.messenger.presentation.base.toPhoneAuthUiText
import com.example.messenger.presentation.base.toUiText
import com.example.messenger.util.Resource
import com.example.messenger.util.VerificationResult
import com.google.firebase.auth.PhoneAuthCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginWithPhoneNumberUseCase: LoginWithPhoneNumberUseCase,
    private val linkPhoneUseCase: LinkPhoneUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val observeSessionStateUseCase: ObserveSessionStateUseCase,
    private val firebaseAuthService: FirebaseAuthService,
    private val presenceManager: PresenceManager,
    private val firebaseMessagingManager: FirebaseMessagingManager,
    private val firestoreService: FirestoreService,
    private val syncCoordinator: SyncCoordinator,
    private val backfillRecentHistoryUseCase: BackfillRecentHistoryUseCase,
) : MviViewModel<AuthUiState, AuthIntent, AuthEffect>(
    initialState = AuthUiState(
        session = if (firebaseAuthService.isAuthenticated()) {
            SessionState.Resolving
        } else {
            SessionState.SignedOut()
        },
    ),
) {
    
    private var pendingUsername: String? = null

    private var pendingIsRegister: Boolean = false

    init {
        Log.d(TAG, "init: isAuthenticated=${firebaseAuthService.isAuthenticated()}")
        observeSession()
    }

    override fun handleIntent(intent: AuthIntent) {
        Log.d(TAG, "handleIntent: ${intent::class.simpleName}")
        when (intent) {
            is AuthIntent.SendVerificationCode ->
                sendVerificationCode(intent.activity, intent.phoneNumber, intent.username, intent.isRegister)
            is AuthIntent.ResendCode -> resendCode(intent.activity, intent.phoneNumber)
            is AuthIntent.VerifyOtpAndLogin -> verifyOtpAndLogin(intent.otpCode)
            is AuthIntent.VerifyOtpAndLink -> verifyOtpAndLink(intent.otpCode)
            AuthIntent.Logout -> logout()
            AuthIntent.ClearError -> setState { copy(error = null) }
            AuthIntent.EditPhoneNumber -> setState { copy(codeSent = false, error = null) }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            observeSessionStateUseCase().collect { session ->
                setState { copy(session = session) }
                (session as? SessionState.SignedOut)?.reason?.let { endInvalidSession(it) }
            }
        }
    }

    private suspend fun endInvalidSession(reason: SessionEndReason) {
        if (!firebaseAuthService.isAuthenticated()) return
        Log.w(TAG, "endInvalidSession: $reason")
        presenceManager.disconnect(viewModelScope)
        logoutUseCase()
        setState { copy(isLoading = false, error = reason.toUiText()) }
    }

    private fun SessionEndReason.toUiText(): UiText = when (this) {
        SessionEndReason.PROFILE_MISSING ->
            UiText.StringResource(R.string.auth_error_account_not_found)

        SessionEndReason.ACCOUNT_DELETED ->
            UiText.StringResource(R.string.auth_error_account_deleted)
    }

    private fun logout() {
        viewModelScope.launch {
            syncCoordinator.stop()
            presenceManager.disconnect(viewModelScope)
            logoutUseCase()
            setState { AuthUiState() }
        }
    }

    private fun sendVerificationCode(
        activity: Activity,
        phoneNumber: String,
        username: String?,
        isRegister: Boolean,
    ) {
        Log.d(TAG, "sendVerificationCode: username='$username' isRegister=$isRegister")
        pendingUsername = username
        pendingIsRegister = isRegister
        setState { copy(isLoading = true, error = null, codeSent = false) }

        viewModelScope.launch {
            val result = firebaseAuthService.sendVerificationCode(phoneNumber, activity)
            Log.d(TAG, "sendVerificationCode: result isSuccess=${result.isSuccess}")

            if (result.isSuccess) {
                when (val verifyResult = result.getOrThrow()) {
                    is VerificationResult.CodeSent -> {
                        Log.d(TAG, "sendVerificationCode: CodeSent")
                        setState {
                            copy(
                                isLoading = false,
                                codeSent = true,
                                verificationId = verifyResult.verificationId,
                            )
                        }
                    }
                    is VerificationResult.AutoVerified -> {
                        Log.d(TAG, "sendVerificationCode: AutoVerified -> signInWithPhone")
                        signInWithPhone(verifyResult.credential)
                    }
                }
            } else {
                Log.e(TAG, "sendVerificationCode: FAILED", result.exceptionOrNull())
                val errorMsg: UiText = result.exceptionOrNull()?.toPhoneAuthUiText()
                    ?: UiText.StringResource(R.string.auth_error_verification_failed)
                setState { copy(isLoading = false, error = errorMsg) }
            }
        }
    }

    private fun resendCode(activity: Activity, phoneNumber: String) {
        Log.d(TAG, "resendCode")
        setState { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val result = firebaseAuthService.resendVerificationCode(
                phoneNumber,
                activity,
                currentState.verificationId,
            )
            Log.d(TAG, "resendCode: result isSuccess=${result.isSuccess}")

            if (result.isSuccess) {
                when (val verifyResult = result.getOrThrow()) {
                    is VerificationResult.CodeSent ->
                        setState {
                            copy(
                                isLoading = false,
                                codeSent = true,
                                verificationId = verifyResult.verificationId,
                            )
                        }
                    is VerificationResult.AutoVerified ->
                        signInWithPhone(verifyResult.credential)
                }
            } else {
                Log.e(TAG, "resendCode: FAILED", result.exceptionOrNull())
                val errorMsg: UiText = result.exceptionOrNull()?.toPhoneAuthUiText()
                    ?: UiText.StringResource(R.string.auth_error_verification_failed)
                setState { copy(isLoading = false, error = errorMsg) }
            }
        }
    }

    private fun verifyOtpAndLogin(otpCode: String) {
        Log.d(TAG, "verifyOtpAndLogin: verifying code")
        viewModelScope.launch {
            val credentialResult = firebaseAuthService.verifyCode(otpCode, currentState.verificationId)
            Log.d(TAG, "verifyOtpAndLogin: verifyCode isSuccess=${credentialResult.isSuccess}")
            if (credentialResult.isSuccess) {
                signInWithPhone(credentialResult.getOrThrow())
            } else {
                Log.e(TAG, "verifyOtpAndLogin: invalid code", credentialResult.exceptionOrNull())
                setState { copy(error = UiText.StringResource(R.string.auth_error_invalid_code)) }
            }
        }
    }

    private fun verifyOtpAndLink(otpCode: String) {
        viewModelScope.launch {
            val credentialResult = firebaseAuthService.verifyCode(otpCode, currentState.verificationId)
            if (credentialResult.isSuccess) {
                linkPhone(credentialResult.getOrThrow())
            } else {
                setState { copy(error = UiText.StringResource(R.string.auth_error_invalid_code)) }
            }
        }
    }

    private fun signInWithPhone(credential: PhoneAuthCredential) {
        Log.d(TAG, "signInWithPhone: start, pendingUsername='$pendingUsername' isRegister=$pendingIsRegister")
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            val result = loginWithPhoneNumberUseCase(credential, pendingUsername, pendingIsRegister)
            Log.d(TAG, "signInWithPhone: loginWithPhoneNumberUseCase -> ${result::class.simpleName}")
            pendingUsername = null
            pendingIsRegister = false
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "signInWithPhone: SUCCESS uid=${result.data.id}")

                    presenceManager.goOnline(viewModelScope)
                    syncCoordinator.restart()
                    restoreRecentHistory()
                    syncFcmTokenForCurrentUser()
                    setState {
                        copy(
                            isLoading = false,
                            currentUser = result.data,
                            error = null,
                        )
                    }
                    Log.d(TAG, "signInWithPhone: emitting AuthSucceeded")
                    emitEffect(AuthEffect.AuthSucceeded)
                }
                is Resource.Error -> {
                    Log.e(TAG, "signInWithPhone: ERROR ${result.message}")
                    setState { copy(isLoading = false, error = result.message.toUiText()) }
                }
                is Resource.Failure -> {
                    Log.e(TAG, "signInWithPhone: FAILURE", result.exception)
                    setState { copy(isLoading = false, error = result.exception.message?.toUiText()) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun restoreRecentHistory() {
        viewModelScope.launch {
            backfillRecentHistoryUseCase()
                .onSuccess { Log.d(TAG, "restoreRecentHistory: pulled $it messages") }
                .onFailure { Log.w(TAG, "restoreRecentHistory failed", it) }
        }
    }

    private suspend fun syncFcmTokenForCurrentUser() {
        val uid = firebaseAuthService.getCurrentUserId()
        Log.d(TAG, "syncFcmTokenForCurrentUser: uid=$uid")
        if (uid == null) return
        firebaseMessagingManager.getFcmToken()
            .onSuccess { token ->
                Log.d(TAG, "syncFcmTokenForCurrentUser: got token, updating Firestore")
                firestoreService.updateFcmToken(uid, token)
            }
            .onFailure { e ->
                Log.w(TAG, "FCM token sync skipped: ${e.message}", e)
            }
    }

    private companion object {
        const val TAG = "AUTHFLOW_VM"
    }

    private fun linkPhone(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            linkPhoneUseCase(credential).collect { resource ->
                when (resource) {
                    is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false, error = null) }
                        emitEffect(AuthEffect.PhoneLinked)
                    }
                    is Resource.Error -> setState { copy(isLoading = false, error = resource.message.toUiText()) }
                    is Resource.Failure -> setState { copy(isLoading = false, error = resource.exception.message?.toUiText()) }
                }
            }
        }
    }
}
