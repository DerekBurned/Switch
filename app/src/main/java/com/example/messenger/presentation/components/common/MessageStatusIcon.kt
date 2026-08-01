package com.example.messenger.presentation.components.common
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.presentation.screens.ui.theme.DeliveredBlue
import com.example.messenger.presentation.screens.ui.theme.FailedRed
import com.example.messenger.presentation.screens.ui.theme.OfflineGray

@Composable
fun MessageStatusIcon(
    status: MessageStatus,
    modifier: Modifier = Modifier
) {
    val iconSize = 16.dp

    when (status) {
        MessageStatus.SENDING -> {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Sending",
                modifier = modifier.size(iconSize),
                tint = OfflineGray
            )
        }
        MessageStatus.SENT -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                modifier = modifier.size(iconSize),
                tint = OfflineGray
            )
        }
        MessageStatus.READ -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Read",
                modifier = modifier.size(iconSize),
                tint = DeliveredBlue
            )
        }
        MessageStatus.FAILED -> {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Failed - tap to retry",
                modifier = modifier.size(iconSize),
                tint = FailedRed
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MessageStatusIconPreview() {
    MessengerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MessageStatusIcon(status = MessageStatus.SENDING)
                MessageStatusIcon(status = MessageStatus.SENT)
                MessageStatusIcon(status = MessageStatus.READ)
                MessageStatusIcon(status = MessageStatus.FAILED)
            }
        }
    }
}
