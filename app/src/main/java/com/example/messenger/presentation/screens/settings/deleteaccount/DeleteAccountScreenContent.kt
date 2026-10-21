package com.example.messenger.presentation.screens.settings.deleteaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.presentation.components.auth.OtpStep
import com.example.messenger.presentation.components.common.MessengerInputField
import com.example.messenger.presentation.components.common.PillButton
import com.example.messenger.presentation.components.common.model.PillButtonStyle
import com.example.messenger.presentation.screens.settings.SettingsSubScaffold
import com.example.messenger.presentation.theme.MessengerTheme
import com.example.messenger.presentation.theme.messengerTokens

@Composable
fun DeleteAccountScreenContent(
    state: DeleteAccountUiState,
    onBack: () -> Unit = {},
    onConfirmationChange: (String) -> Unit = {},
    onOtpChange: (String) -> Unit = {},
    onStartDeletion: () -> Unit = {},
    onVerify: () -> Unit = {},
) {
    val tokens = messengerTokens

    SettingsSubScaffold(title = "Delete account", onBack = onBack) {
        when (state.step) {
            DeleteAccountStep.CONFIRM -> {
                ConsequencesCard()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Type ${DeleteAccountUiState.CONFIRMATION_WORD} to confirm.",
                    color = tokens.textMuted,
                    fontSize = 14.sp,
                )
                MessengerInputField(
                    value = state.confirmation,
                    onValueChange = onConfirmationChange,
                    placeholder = DeleteAccountUiState.CONFIRMATION_WORD,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                    ),
                )
                Spacer(modifier = Modifier.height(8.dp))
                PillButton(
                    text = "Delete my account",
                    onClick = onStartDeletion,
                    style = PillButtonStyle.Danger,
                    enabled = state.isConfirmed && !state.isLoading,
                    loading = state.isLoading,
                )
            }

            DeleteAccountStep.VERIFY -> OtpStep(
                label = "Enter the code we sent to ${state.phone} to confirm it is you.",
                otp = state.otp,
                onOtpChange = onOtpChange,
                isLoading = state.isLoading,
                onVerify = onVerify,
            )
        }

        state.error?.let { error ->
            Text(error.asString(), color = tokens.danger, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ConsequencesCard() {
    val tokens = messengerTokens
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(tokens.danger.copy(alpha = 0.12f))
            .padding(20.dp),
    ) {
        Text(
            text = "This cannot be undone",
            color = tokens.danger,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Consequence("Your profile, photo and encryption keys are erased.")
        Consequence("You are signed out everywhere and this device is wiped.")
        Consequence("Your number is released, so registering again creates a new account with no access to this history.")
        Consequence("People you have chatted with keep those conversations, but you appear as an unknown user.")
    }
}

@Composable
private fun Consequence(text: String) {
    Text(
        text = "•  $text",
        color = messengerTokens.textPrimary,
        fontSize = 14.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Preview(name = "Confirm", showBackground = true, showSystemUi = true)
@Composable
private fun DeleteAccountConfirmPreview() {
    MessengerTheme {
        DeleteAccountScreenContent(state = DeleteAccountUiState(confirmation = "DELETE"))
    }
}

@Preview(name = "Verify", showBackground = true, showSystemUi = true)
@Composable
private fun DeleteAccountVerifyPreview() {
    MessengerTheme {
        DeleteAccountScreenContent(
            state = DeleteAccountUiState(
                step = DeleteAccountStep.VERIFY,
                phone = "+1 555 0100",
                otp = "123",
            ),
        )
    }
}
