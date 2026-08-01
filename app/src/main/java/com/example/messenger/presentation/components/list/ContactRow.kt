package com.example.messenger.presentation.components.list
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.domain.model.User
import com.example.messenger.presentation.components.common.pressScale
import com.example.messenger.presentation.screens.ui.theme.LightGray
import com.example.messenger.presentation.screens.ui.theme.PrimaryBlue

@Composable
fun ContactRow(user: User, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .pressScale(interaction, pressedScale = 0.98f)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
            ) { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(LightGray),
            contentAlignment = Alignment.Center,
        ) {
            val initial = user.username?.take(1)?.uppercase()
            if (!initial.isNullOrBlank()) {
                Text(initial, fontWeight = FontWeight.Bold, color = PrimaryBlue)
            } else {
                Icon(Icons.Filled.Person, contentDescription = null, tint = PrimaryBlue)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = user.username ?: "Unknown",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Text(
                text = user.email ?: user.phoneNumber?.getFullNumber() ?: "",
                color = Color.Gray,
                fontSize = 12.sp,
            )
        }
    }
    HorizontalDivider(color = LightGray, modifier = Modifier.padding(start = 72.dp))
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContactRowPreview() {
    MessengerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ContactRow(user = User(id = "1", username = "Alice", email = "alice@example.com"), onClick = {})
        }
    }
}
