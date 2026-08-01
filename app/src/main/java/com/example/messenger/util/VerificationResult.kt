package com.example.messenger.util

import com.google.firebase.auth.PhoneAuthCredential

sealed class VerificationResult {
    data class CodeSent(val verificationId: String) : VerificationResult()
    data class AutoVerified(val credential: PhoneAuthCredential) : VerificationResult()
}