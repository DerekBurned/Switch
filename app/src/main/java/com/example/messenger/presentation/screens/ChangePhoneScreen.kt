package com.example.messenger.presentation.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.presentation.components.call.CallAwareTopBar
import com.example.messenger.presentation.base.ObserveAsEvents
import com.example.messenger.presentation.components.auth.ConfirmCurrentStep
import com.example.messenger.presentation.components.auth.EnterNewStep
import com.example.messenger.presentation.components.auth.OtpStep
import com.example.messenger.presentation.components.auth.StepIndicator
import com.example.messenger.presentation.components.common.NavHeaderPill
import com.example.messenger.presentation.effect.ChangePhoneEffect
import com.example.messenger.presentation.intent.ChangePhoneIntent
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import com.example.messenger.presentation.state.ChangePhoneStep
import com.example.messenger.presentation.viewmodel.ChangePhoneViewModel

@Composable
fun ChangePhoneScreen(
    viewModel: ChangePhoneViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onDone: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val tokens = messengerTokens

    ObserveAsEvents(viewModel.effect) { effect ->
        when (effect) {
            ChangePhoneEffect.Done -> onDone()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
    ) {
        CallAwareTopBar {
            NavHeaderPill(
                title = when (state.step) {
                    ChangePhoneStep.CONFIRM_CURRENT,
                    ChangePhoneStep.VERIFY_CURRENT,
                    -> "Verify current number"
                    ChangePhoneStep.ENTER_NEW,
                    ChangePhoneStep.VERIFY_NEW,
                    -> "Set new number"
                },
                onBack = {
                    if (state.step == ChangePhoneStep.CONFIRM_CURRENT) onBackClick()
                    else viewModel.dispatch(ChangePhoneIntent.GoBack)
                },
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 28.dp),
        ) {
            StepIndicator(step = state.step)
            Spacer(modifier = Modifier.height(20.dp))

            when (state.step) {
                ChangePhoneStep.CONFIRM_CURRENT -> ConfirmCurrentStep(
                    currentPhone = state.currentPhone,
                    isLoading = state.isLoading,
                    onSendCode = {
                        activity?.let { viewModel.dispatch(ChangePhoneIntent.SendCodeToCurrentPhone(it)) }
                    },
                )
                ChangePhoneStep.VERIFY_CURRENT -> OtpStep(
                    label = "Enter the code we sent to ${state.currentPhone}",
                    otp = state.otp,
                    onOtpChange = { viewModel.dispatch(ChangePhoneIntent.OtpChange(it)) },
                    isLoading = state.isLoading,
                    onVerify = { viewModel.dispatch(ChangePhoneIntent.VerifyCurrentOtp) },
                )
                ChangePhoneStep.ENTER_NEW -> EnterNewStep(
                    state = state,
                    onCountrySelected = { viewModel.dispatch(ChangePhoneIntent.NewCountrySelected(it)) },
                    onNumberChange = { viewModel.dispatch(ChangePhoneIntent.NewNumberChange(it)) },
                    onSendCode = {
                        activity?.let { viewModel.dispatch(ChangePhoneIntent.SendCodeToNewPhone(it)) }
                    },
                )
                ChangePhoneStep.VERIFY_NEW -> OtpStep(
                    label = "Enter the code we sent to " +
                        "${state.newCountry.dialCode} ${state.newNationalNumber}",
                    otp = state.otp,
                    onOtpChange = { viewModel.dispatch(ChangePhoneIntent.OtpChange(it)) },
                    isLoading = state.isLoading,
                    onVerify = { viewModel.dispatch(ChangePhoneIntent.VerifyNewOtp) },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            state.error?.let { err ->
                Text(err.asString(), color = tokens.danger, fontSize = 13.sp)
            }
        }
    }
}
