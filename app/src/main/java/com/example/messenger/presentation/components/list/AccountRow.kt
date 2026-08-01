package com.example.messenger.presentation.components.list
import com.example.messenger.presentation.components.common.MessengerAvatar
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Surface
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.messenger.presentation.components.common.pressScale
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import com.example.messenger.presentation.state.AccountSummary

@Composable
fun AccountRow(account: AccountSummary, isCurrent: Boolean, onClick: () -> Unit) {
    val tokens = messengerTokens
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction, pressedScale = 0.98f)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MessengerAvatar(name = account.name, size = 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(account.name, color = tokens.textOnField, style = MaterialTheme.typography.titleSmall)
            Text(account.phone, color = tokens.textMuted, style = MaterialTheme.typography.bodySmall)
        }
        if (isCurrent) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Current account",
                tint = tokens.accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AccountRowPreview() {
    MessengerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                AccountRow(AccountSummary("a1", "Alice", "+1 555 0100"), isCurrent = true, onClick = {})
                AccountRow(AccountSummary("a2", "Bob Work", "+1 555 0200"), isCurrent = false, onClick = {})
            }
        }
    }
}
