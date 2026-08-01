package com.example.messenger.presentation.components.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.presentation.components.common.PillButton
import com.example.messenger.presentation.screens.ui.theme.PrimaryBlue
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import com.example.messenger.presentation.state.ChangePhoneStep
import com.example.messenger.presentation.state.ChangePhoneUiState
import androidx.compose.material3.Surface
import androidx.compose.ui.tooling.preview.Preview
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme

@Composable
fun StepIndicator(step: ChangePhoneStep) {
    val currentIndex = when (step) {
        ChangePhoneStep.CONFIRM_CURRENT, ChangePhoneStep.VERIFY_CURRENT -> 0
        ChangePhoneStep.ENTER_NEW -> 1
        ChangePhoneStep.VERIFY_NEW -> 2
    }
    val labels = listOf("Verify current", "New number", "Verify new")
    Column {
        Text(
            text = "Step ${currentIndex + 1} of 3 · ${labels[currentIndex]}",
            color = messengerTokens.textMuted,
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            color = if (i <= currentIndex) PrimaryBlue else PrimaryBlue.copy(alpha = 0.15f),
                            shape = CircleShape,
                        ),
                )
            }
        }
    }
}

@Composable
fun ConfirmCurrentStep(
    currentPhone: String,
    isLoading: Boolean,
    onSendCode: () -> Unit,
) {
    Text(
        text = "To change your phone number we first need to verify it's still you.",
        color = messengerTokens.textMuted,
        fontSize = 14.sp,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Current number",
        color = messengerTokens.textMuted,
        fontSize = 12.sp,
    )
    Text(
        text = currentPhone.ifBlank { "(none on file)" },
        color = messengerTokens.textPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(24.dp))
    PrimaryActionButton(
        text = "Send verification code",
        enabled = !isLoading && currentPhone.isNotBlank(),
        isLoading = isLoading,
        onClick = onSendCode,
    )
}

@Composable
fun EnterNewStep(
    state: ChangePhoneUiState,
    onCountrySelected: (Country) -> Unit,
    onNumberChange: (String) -> Unit,
    onSendCode: () -> Unit,
) {
    Text(
        text = "Enter your new phone number. We'll send a code to confirm it.",
        color = messengerTokens.textMuted,
        fontSize = 14.sp,
    )
    Spacer(modifier = Modifier.height(20.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CountryCodePicker(
            selected = state.newCountry,
            onCountrySelected = onCountrySelected,
        )
        Box(modifier = Modifier.weight(1f)) {
            AuthInputTextField(
                value = state.newNationalNumber,
                onValueChange = onNumberChange,
                placeholder = "phone number",
                keyboardType = KeyboardType.Phone,
            )
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
    PrimaryActionButton(
        text = "Send code",
        enabled = !state.isLoading && state.newNationalNumber.isNotBlank(),
        isLoading = state.isLoading,
        onClick = onSendCode,
    )
}

@Composable
fun OtpStep(
    label: String,
    otp: String,
    onOtpChange: (String) -> Unit,
    isLoading: Boolean,
    onVerify: () -> Unit,
) {
    Text(label, color = messengerTokens.textMuted, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(20.dp))
    AuthInputTextField(
        value = otp,
        onValueChange = onOtpChange,
        placeholder = "SMS code",
        keyboardType = KeyboardType.Number,
    )
    Spacer(modifier = Modifier.height(24.dp))
    PrimaryActionButton(
        text = "Verify",
        enabled = !isLoading && otp.isNotBlank(),
        isLoading = isLoading,
        onClick = onVerify,
    )
}

@Composable
private fun PrimaryActionButton(
    text: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    PillButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        loading = isLoading,
    )
}

@Preview
@Composable
private fun ChangePhoneComponentsPreview() {
    MessengerTheme {
        Surface {
            Column {
                StepIndicator(step = ChangePhoneStep.ENTER_NEW)
                Spacer(modifier = Modifier.height(20.dp))
                ConfirmCurrentStep(currentPhone = "+1 555 0100", isLoading = false, onSendCode = {})
                Spacer(modifier = Modifier.height(20.dp))
                OtpStep(label = "Enter the code we sent", otp = "123", onOtpChange = {}, isLoading = false, onVerify = {})
            }
        }
    }
}
