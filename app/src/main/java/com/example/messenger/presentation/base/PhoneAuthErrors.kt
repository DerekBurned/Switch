package com.example.messenger.presentation.base

import androidx.annotation.StringRes
import com.example.messenger.R
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException

fun Throwable.toPhoneAuthUiText(
    @StringRes fallback: Int = R.string.auth_error_verification_failed,
): UiText = when (this) {
    is FirebaseTooManyRequestsException ->
        UiText.StringResource(R.string.auth_error_too_many_requests)

    is FirebaseAuthException -> when (errorCode) {
        "ERROR_WEB_CONTEXT_CANCELED",
        "ERROR_USER_CANCELLED",
        -> UiText.StringResource(R.string.auth_error_verification_cancelled)

        "ERROR_WEB_CONTEXT_ALREADY_PRESENTED" ->
            UiText.StringResource(R.string.auth_error_verification_in_progress)

        "ERROR_WEB_INTERNAL_ERROR",
        "ERROR_WEB_STORAGE_UNSUPPORTED",
        -> UiText.StringResource(R.string.auth_error_verification_unavailable)

        "ERROR_MISSING_CLIENT_IDENTIFIER",
        "ERROR_APP_NOT_AUTHORIZED",
        "ERROR_INVALID_CERT_HASH",
        "ERROR_UNAUTHORIZED_DOMAIN",
        -> UiText.StringResource(R.string.auth_error_app_not_verified)

        "ERROR_QUOTA_EXCEEDED" ->
            UiText.StringResource(R.string.auth_error_too_many_requests)

        "ERROR_INVALID_PHONE_NUMBER",
        "ERROR_MISSING_PHONE_NUMBER",
        -> UiText.StringResource(R.string.auth_error_invalid_number)

        "ERROR_INVALID_VERIFICATION_CODE",
        "ERROR_MISSING_VERIFICATION_CODE",
        -> UiText.StringResource(R.string.auth_error_invalid_code)

        "ERROR_SESSION_EXPIRED" ->
            UiText.StringResource(R.string.auth_error_code_expired)

        else -> message?.toUiText() ?: UiText.StringResource(fallback)
    }

    else -> message?.toUiText() ?: UiText.StringResource(fallback)
}
