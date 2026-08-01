package com.example.messenger.presentation

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.R
import com.example.messenger.data.remote.call.ActiveCallHolder
import com.example.messenger.data.remote.call.CallForegroundService
import com.example.messenger.presentation.screens.ui.theme.ChatBackground
import com.example.messenger.presentation.screens.ui.theme.DangerRed
import com.example.messenger.presentation.screens.ui.theme.OnlineGreen
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IncomingCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        if (intent.getBooleanExtra(MainActivity.EXTRA_ACCEPT_CALL, false)) {
            accept()
            return
        }

        setContent {
            MessengerTheme {
                val call by ActiveCallHolder.state.collectAsStateWithLifecycle()
                val current = call
                LaunchedEffect(current == null, current?.isActive) {
                    if (current == null || current.isActive) finish()
                }
                if (current != null && !current.isActive) {
                    IncomingCallUi(
                        partnerName = current.partnerName,
                        isVideo = current.isVideoCall,
                        onAccept = ::accept,
                        onDecline = ::decline,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(MainActivity.EXTRA_ACCEPT_CALL, false)) accept()
    }

    private fun accept() {
        getSystemService<KeyguardManager>()?.requestDismissKeyguard(this, null)
        val forward = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_OPEN_INCOMING_CALL, true)
            putExtra(MainActivity.EXTRA_ACCEPT_CALL, true)
        }
        startActivity(forward)
        finish()
    }

    private fun decline() {
        val intent = Intent(this, CallForegroundService::class.java)
            .setAction(CallForegroundService.ACTION_DECLINE)
        runCatching { startService(intent) }
        finish()
    }
}

@Composable
private fun IncomingCallUi(
    partnerName: String,
    isVideo: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val name = partnerName.ifBlank { stringResourceCompat() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatBackground)
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(64.dp))
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = name,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(
                if (isVideo) R.string.call_incoming_video_subtitle else R.string.call_incoming_subtitle,
            ),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            CallActionButton(
                color = DangerRed,
                icon = Icons.Filled.CallEnd,
                contentDescription = androidx.compose.ui.res.stringResource(R.string.call_action_decline),
                onClick = onDecline,
            )
            CallActionButton(
                color = OnlineGreen,
                icon = Icons.Filled.Call,
                contentDescription = androidx.compose.ui.res.stringResource(R.string.call_action_accept),
                onClick = onAccept,
            )
        }
    }
}

@Composable
private fun CallActionButton(
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = color,
            contentColor = Color.White,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
private fun stringResourceCompat(): String =
    androidx.compose.ui.res.stringResource(R.string.notif_call_caller_fallback)
