package com.example.messenger.presentation.components.list
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.domain.model.User
import com.example.messenger.presentation.components.common.MessengerAvatar
import com.example.messenger.presentation.components.common.pressScale
import com.example.messenger.presentation.theme.MessengerTheme
import com.example.messenger.presentation.theme.messengerTokens

@Composable
fun SearchUserItem(
    user: User,
    onClick: () -> Unit,
) {
    val tokens = messengerTokens
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .pressScale(interaction, pressedScale = 0.98f)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MessengerAvatar(name = user.username.orEmpty(), size = 48.dp)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = user.username ?: "Unknown",
                color = tokens.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Text(
                text = user.email ?: "",
                color = tokens.textMuted,
                fontSize = 14.sp,
            )
        }
    }
    HorizontalDivider(color = tokens.divider, thickness = 1.dp, modifier = Modifier.padding(start = 80.dp))
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchUserItemPreview() {
    MessengerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SearchUserItem(user = User(id = "1", username = "Alice", email = "alice@example.com"), onClick = {})
        }
    }
}
