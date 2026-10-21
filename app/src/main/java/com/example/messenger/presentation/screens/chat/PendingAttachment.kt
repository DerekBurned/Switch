package com.example.messenger.presentation.screens.chat

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class PendingAttachment(val uri: Uri, val kind: String)
