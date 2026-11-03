package com.example.messenger.presentation.screens.auth

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.presentation.base.ObserveAsEvents
import com.example.messenger.presentation.components.auth.PhoneNumberRules
import com.example.messenger.presentation.components.auth.model.AuthMode

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onAuthSuccess: () -> Unit = {},
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val phoneRules = remember { PhoneNumberRules() }

    var attempt by remember { mutableIntStateOf(0) }
    val ring = remember { Animatable(1f) }
    var expired by remember { mutableStateOf(false) }

    val activity = LocalActivity.current

    LaunchedEffect(uiState.codeSent, attempt) {
        if (uiState.codeSent) {
            expired = false
            ring.snapTo(1f)
            ring.animateTo(0f, animationSpec = tween(durationMillis = 120_000, easing = LinearEasing))
            expired = true
        } else {
            ring.snapTo(1f)
            expired = false
        }
    }

    ObserveAsEvents(viewModel.effect) { effect ->
        when (effect) {
            AuthEffect.AuthSucceeded -> onAuthSuccess()
            AuthEffect.PhoneLinked -> Unit
        }
    }

    AuthScreenContent(
        mode = uiState.mode,
        selectedCountry = uiState.country,
        nationalNumber = uiState.nationalNumber,
        phoneValid = uiState.phoneValid,
        username = uiState.username,
        otp = uiState.otp,
        isLoading = uiState.isLoading,
        error = uiState.error?.asString(),
        codeSent = uiState.codeSent,
        ringProgress = { ring.value },
        expired = expired,
        onModeChange = { newMode ->
            if (newMode != uiState.mode) viewModel.dispatch(AuthIntent.SelectMode(newMode))
        },
        onCountryChange = { viewModel.dispatch(AuthIntent.SelectCountry(it)) },
        onNationalNumberChange = { viewModel.dispatch(AuthIntent.ChangeNationalNumber(it)) },
        onChangeNumber = { viewModel.dispatch(AuthIntent.EditPhoneNumber) },
        onUsernameChange = { viewModel.dispatch(AuthIntent.ChangeUsername(it)) },
        onOtpChange = { viewModel.dispatch(AuthIntent.ChangeOtp(it)) },
        onSendOtp = {
            val fullPhone = phoneRules.toE164(uiState.country, uiState.nationalNumber)
                ?: return@AuthScreenContent
            val isRegister = uiState.mode == AuthMode.REGISTER
            val nameForVm = if (isRegister) uiState.username else null
            activity?.let {
                viewModel.dispatch(AuthIntent.SendVerificationCode(it, fullPhone, nameForVm, isRegister))
            }
        },
        onVerifyOtp = { viewModel.dispatch(AuthIntent.VerifyOtpAndLogin(uiState.otp)) },
        onResendCode = {
            val fullPhone = phoneRules.toE164(uiState.country, uiState.nationalNumber)
                ?: return@AuthScreenContent
            activity?.let {
                attempt++
                viewModel.dispatch(AuthIntent.ResendCode(it, fullPhone))
            }
        },
    )
}
