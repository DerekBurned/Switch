package com.example.messenger.presentation.notification

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.example.messenger.R
import kotlinx.coroutines.delay

@Composable
fun NotificationPermissionGate(isAuthenticated: Boolean) {
    NotificationsPermissionPrompt(isAuthenticated)
    FullScreenIntentPermissionPrompt(isAuthenticated)
}

@Composable
private fun NotificationsPermissionPrompt(isAuthenticated: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    var showRationale by rememberSaveable { mutableStateOf(false) }
    var asked by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated || asked) return@LaunchedEffect
        delay(LANDING_SETTLE_MS)
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) showRationale = true
        asked = true
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(stringResource(R.string.notif_permission_rationale_title)) },
            text = { Text(stringResource(R.string.notif_permission_rationale_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) {
                    Text(stringResource(R.string.notif_permission_rationale_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text(stringResource(R.string.notif_permission_rationale_not_now))
                }
            },
        )
    }
}

@Composable
private fun FullScreenIntentPermissionPrompt(isAuthenticated: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return

    val context = LocalContext.current
    var showRationale by rememberSaveable { mutableStateOf(false) }
    var asked by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated || asked) return@LaunchedEffect
        delay(LANDING_SETTLE_MS + FSI_EXTRA_DELAY_MS)
        val nm = context.getSystemService<NotificationManager>()
        if (nm != null && !nm.canUseFullScreenIntent()) showRationale = true
        asked = true
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(stringResource(R.string.fsi_rationale_title)) },
            text = { Text(stringResource(R.string.fsi_rationale_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:${context.packageName}"),
                    )
                    runCatching { context.startActivity(intent) }
                }) {
                    Text(stringResource(R.string.fsi_rationale_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text(stringResource(R.string.notif_permission_rationale_not_now))
                }
            },
        )
    }
}

private const val LANDING_SETTLE_MS = 700L
private const val FSI_EXTRA_DELAY_MS = 500L
