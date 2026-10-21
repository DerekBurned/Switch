package com.example.messenger.presentation.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.messenger.presentation.components.auth.CountryCodePicker
import com.example.messenger.presentation.components.auth.model.AuthMode
import com.example.messenger.presentation.components.auth.model.Countries
import com.example.messenger.presentation.components.auth.model.Country
import com.example.messenger.presentation.components.common.MessengerInputField
import com.example.messenger.presentation.components.common.OtpCodeField
import com.example.messenger.presentation.components.common.PillButton
import com.example.messenger.presentation.components.common.SegmentedToggle
import com.example.messenger.presentation.components.common.WallpaperBackground
import com.example.messenger.presentation.components.common.model.PillButtonStyle
import com.example.messenger.presentation.theme.MessengerTheme
import com.example.messenger.presentation.theme.Motion
import com.example.messenger.presentation.theme.messengerTokens

@Composable
internal fun AuthScreenContent(
    mode: AuthMode,
    selectedCountry: Country,
    nationalNumber: String,
    username: String,
    otp: String,
    isLoading: Boolean,
    error: String?,
    codeSent: Boolean,
    ringProgress: () -> Float = { 1f },
    expired: Boolean = false,
    onModeChange: (AuthMode) -> Unit,
    onCountryChange: (Country) -> Unit,
    onNationalNumberChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onOtpChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    onVerifyOtp: () -> Unit,
    onResendCode: () -> Unit = {},
) {
    val tokens = messengerTokens
    val sendEnabled = !isLoading && nationalNumber.isNotBlank() &&
        (mode == AuthMode.LOGIN || username.isNotBlank())
    val buttonEnabled = if (codeSent) !isLoading && otp.isNotBlank() else sendEnabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = !codeSent,
            enter = expandVertically(tween(Motion.durationMedium, easing = Motion.emphasized)) +
                fadeIn(tween(Motion.durationMedium)),
            exit = shrinkVertically(tween(Motion.durationMedium, easing = Motion.emphasized)) +
                fadeOut(tween(Motion.durationMedium)),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SegmentedToggle(
                    options = listOf("Register", "Login"),
                    selectedIndex = if (mode == AuthMode.REGISTER) 0 else 1,
                    onSelect = { onModeChange(if (it == 0) AuthMode.REGISTER else AuthMode.LOGIN) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        AnimatedVisibility(
            visible = mode == AuthMode.REGISTER && !codeSent,
            enter = expandVertically(
                animationSpec = tween(Motion.durationMedium, easing = Motion.emphasized),
                expandFrom = Alignment.Bottom,
            ) + fadeIn(tween(Motion.durationMedium)) + scaleIn(
                animationSpec = tween(Motion.durationMedium, easing = Motion.emphasized),
                initialScale = 0.85f,
                transformOrigin = TransformOrigin(0.5f, 1f),
            ),
            exit = shrinkVertically(
                animationSpec = tween(Motion.durationMedium, easing = Motion.emphasized),
                shrinkTowards = Alignment.Bottom,
            ) + fadeOut(tween(Motion.durationMedium)) + scaleOut(
                animationSpec = tween(Motion.durationMedium, easing = Motion.emphasized),
                targetScale = 0.85f,
                transformOrigin = TransformOrigin(0.5f, 1f),
            ),
        ) {
            Column {
                MessengerInputField(
                    value = username,
                    onValueChange = onUsernameChange,
                    placeholder = "Enter your username",
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CountryCodePicker(
                selected = selectedCountry,
                onCountrySelected = onCountryChange,
            )
            Box(modifier = Modifier.weight(1f)) {
                MessengerInputField(
                    value = nationalNumber,
                    onValueChange = onNationalNumberChange,
                    placeholder = "Enter phone number",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
            }
        }

        AnimatedVisibility(
            visible = codeSent,
            enter = expandVertically(
                animationSpec = tween(Motion.durationMedium, easing = Motion.emphasized),
                expandFrom = Alignment.Top,
            ) + fadeIn(tween(Motion.durationMedium)) + scaleIn(
                animationSpec = tween(Motion.durationMedium, easing = Motion.emphasized),
                initialScale = 0.85f,
                transformOrigin = TransformOrigin(0.5f, 0f),
            ),
            exit = shrinkVertically(
                animationSpec = tween(Motion.durationMedium, easing = Motion.emphasized),
                shrinkTowards = Alignment.Top,
            ) + fadeOut(tween(Motion.durationMedium)) + scaleOut(
                animationSpec = tween(Motion.durationMedium, easing = Motion.emphasized),
                targetScale = 0.85f,
                transformOrigin = TransformOrigin(0.5f, 0f),
            ),
        ) {
            Column {
                Spacer(modifier = Modifier.height(14.dp))
                OtpCodeField(
                    value = otp,
                    onValueChange = onOtpChange,
                    ringProgress = ringProgress,
                    expired = expired,
                    onRetry = onResendCode,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Code sent to ${selectedCountry.dialCode} $nationalNumber",
                    color = tokens.textPrimary.copy(alpha = 0.6f),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error,
                color = tokens.danger,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
        PillButton(
            text = when {
                codeSent -> "Verify code"
                mode == AuthMode.LOGIN -> "Log in"
                else -> "Sign in"
            },
            onClick = { if (codeSent) onVerifyOtp() else onSendOtp() },
            style = PillButtonStyle.Neutral,
            enabled = buttonEnabled,
            loading = isLoading,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AuthScreenLoginPreview() {
    MessengerTheme {
        WallpaperBackground {
            AuthScreenContent(
                mode = AuthMode.LOGIN,
                selectedCountry = Countries.all.first { it.isoCode == "US" },
                nationalNumber = "",
                username = "",
                otp = "",
                isLoading = false,
                error = null,
                codeSent = false,
                onModeChange = {},
                onCountryChange = {},
                onNationalNumberChange = {},
                onUsernameChange = {},
                onOtpChange = {},
                onSendOtp = {},
                onVerifyOtp = {},
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AuthScreenRegisterPreview() {
    MessengerTheme {
        WallpaperBackground {
            AuthScreenContent(
                mode = AuthMode.REGISTER,
                selectedCountry = Countries.all.first { it.isoCode == "PL" },
                nationalNumber = "123456789",
                username = "derek",
                otp = "",
                isLoading = false,
                error = null,
                codeSent = false,
                onModeChange = {},
                onCountryChange = {},
                onNationalNumberChange = {},
                onUsernameChange = {},
                onOtpChange = {},
                onSendOtp = {},
                onVerifyOtp = {},
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AuthScreenCodeSentPreview() {
    MessengerTheme {
        WallpaperBackground {
            AuthScreenContent(
                mode = AuthMode.REGISTER,
                selectedCountry = Countries.all.first { it.isoCode == "PL" },
                nationalNumber = "123456789",
                username = "derek",
                otp = "123",
                isLoading = false,
                error = null,
                codeSent = true,
                ringProgress = { 0.6f },
                expired = false,
                onModeChange = {},
                onCountryChange = {},
                onNationalNumberChange = {},
                onUsernameChange = {},
                onOtpChange = {},
                onSendOtp = {},
                onVerifyOtp = {},
            )
        }
    }
}
