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
import com.example.messenger.presentation.components.auth.model.AuthMode
import com.example.messenger.presentation.components.auth.model.Countries

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onAuthSuccess: () -> Unit = {},
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    var mode by remember { mutableStateOf(AuthMode.REGISTER) }
    var selectedCountry by remember { mutableStateOf(Countries.default) }
    var nationalNumber by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }

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
        mode = mode,
        selectedCountry = selectedCountry,
        nationalNumber = nationalNumber,
        username = username,
        otp = otp,
        isLoading = uiState.isLoading,
        error = uiState.error?.asString(),
        codeSent = uiState.codeSent,
        ringProgress = { ring.value },
        expired = expired,
        onModeChange = { newMode ->
            if (newMode != mode) {
                mode = newMode
                if (newMode == AuthMode.LOGIN) username = ""
                viewModel.dispatch(AuthIntent.ClearError)
            }
        },
        onCountryChange = { selectedCountry = it },
        onNationalNumberChange = {
            nationalNumber = it.filter { ch -> ch.isDigit() }
            if (uiState.codeSent) {
                otp = ""
                viewModel.dispatch(AuthIntent.EditPhoneNumber)
            }
        },
        onUsernameChange = { username = it },
        onOtpChange = { otp = it },
        onSendOtp = {
            val fullPhone = selectedCountry.dialCode + nationalNumber
            val isRegister = mode == AuthMode.REGISTER
            val nameForVm = if (isRegister) username else null
            activity?.let {
                viewModel.dispatch(AuthIntent.SendVerificationCode(it, fullPhone, nameForVm, isRegister))
            }
        },
        onVerifyOtp = { viewModel.dispatch(AuthIntent.VerifyOtpAndLogin(otp)) },
        onResendCode = {
            val fullPhone = selectedCountry.dialCode + nationalNumber
            activity?.let {
                attempt++
                viewModel.dispatch(AuthIntent.ResendCode(it, fullPhone))
            }
        },
    )
}
