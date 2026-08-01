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
import androidx.compose.material3.HorizontalDivider
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
fun SearchUserItem(
    user: User,
    onClick: () -> Unit,
) {
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
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(LightGray),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = user.username?.take(1)?.uppercase() ?: "?",
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = user.username ?: "Unknown",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Text(
                text = user.email ?: "",
                color = Color.Gray,
                fontSize = 14.sp,
            )
        }
    }
    HorizontalDivider(color = LightGray, thickness = 1.dp, modifier = Modifier.padding(start = 80.dp))
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
