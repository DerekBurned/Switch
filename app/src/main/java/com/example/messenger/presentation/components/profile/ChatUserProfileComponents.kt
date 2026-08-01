package com.example.messenger.presentation.components.profile

import com.example.messenger.presentation.components.common.MessengerAvatar
import com.example.messenger.presentation.components.common.sharedElementKey
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import com.example.messenger.presentation.state.ChatUserProfileUiState
import com.example.messenger.presentation.state.MediaTab
import androidx.compose.material3.Surface
import androidx.compose.ui.tooling.preview.Preview
import com.example.messenger.domain.model.PhoneNumber
import com.example.messenger.domain.model.User
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme

@Composable
fun ProfileHeader(
    state: ChatUserProfileUiState,
    sharedKeyPartnerId: String = "",
    onAvatarClick: () -> Unit = {},
    onCallClick: (partnerId: String, partnerName: String, partnerPhone: String) -> Unit,
    onVideoCallClick: (partnerId: String, partnerName: String, partnerPhone: String) -> Unit = { _, _, _ -> },
) {
    val tokens = messengerTokens
    val sharedAvatarModifier =
        if (sharedKeyPartnerId.isNotBlank()) Modifier.sharedElementKey("avatar-$sharedKeyPartnerId") else Modifier
    val sharedNameModifier =
        if (sharedKeyPartnerId.isNotBlank()) Modifier.sharedElementKey("name-$sharedKeyPartnerId") else Modifier
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val displayName = state.displayName.ifBlank { state.user?.username.orEmpty() }
        MessengerAvatar(
            name = displayName,
            photoUrl = state.seededAvatarUrl ?: state.user?.avatarUrl ?: state.avatarPhotos.firstOrNull(),
            size = 112.dp,
            modifier = sharedAvatarModifier.clickable(onClick = onAvatarClick),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = displayName.ifBlank { "Unknown" },
            color = tokens.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = sharedNameModifier,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (state.isOnline) "online" else state.lastSeen.ifBlank { "offline" },
            color = if (state.isOnline) tokens.callAccept else tokens.textPrimary.copy(alpha = 0.6f),
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            ProfileActionButton(icon = Icons.Default.Call, contentDescription = "Call") {
                val user = state.user ?: return@ProfileActionButton
                onCallClick(
                    user.id,
                    user.username.orEmpty(),
                    user.phoneNumber?.getFullNumber().orEmpty(),
                )
            }
            ProfileActionButton(icon = Icons.Default.Videocam, contentDescription = "Video call") {
                val user = state.user ?: return@ProfileActionButton
                onVideoCallClick(
                    user.id,
                    user.username.orEmpty(),
                    user.phoneNumber?.getFullNumber().orEmpty(),
                )
            }
            ProfileActionButton(icon = Icons.Default.Notifications, contentDescription = "Mute") { }
            ProfileActionButton(icon = Icons.Default.Block, contentDescription = "Block", danger = true) { }
        }
    }
}

@Composable
private fun ProfileActionButton(
    icon: ImageVector,
    contentDescription: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val tokens = messengerTokens
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(tokens.cardFill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (danger) tokens.danger else tokens.accent,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
fun ProfileInfoCard(state: ChatUserProfileUiState) {
    val tokens = messengerTokens
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.cardFill),
    ) {
        ContactInfoRow("Date of birth", "—", divider = true)
        ContactInfoRow(
            "Phone number",
            state.user?.phoneNumber?.getFullNumber().orEmpty().ifBlank { "—" },
            divider = true,
        )
        ContactInfoRow("Username", "@${state.user?.username.orEmpty()}", divider = true)
        ContactInfoRow("Bio", "—", divider = false)
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun ContactInfoRow(label: String, value: String, divider: Boolean) {
    val tokens = messengerTokens
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(label, color = tokens.textOnField.copy(alpha = 0.6f), fontSize = 11.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = tokens.textOnField, fontSize = 14.sp)
    }
    if (divider) HorizontalDivider(color = tokens.divider, thickness = 0.5.dp)
}

@Composable
fun MediaTabsRow(active: MediaTab, onChange: (MediaTab) -> Unit) {
    val tokens = messengerTokens
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MediaTab.entries.forEach { tab ->
            val isActive = tab == active
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isActive) tokens.accent else tokens.cardFill)
                    .clickable { onChange(tab) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                    color = if (isActive) tokens.onAccent else tokens.textOnField.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
fun ProfileMediaGrid() {
    val tokens = messengerTokens
    val cellColor = if (tokens.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        repeat(2) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(cellColor),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
    Spacer(Modifier.height(24.dp))
}

@Preview
@Composable
private fun ChatUserProfileComponentsPreview() {
    val state = ChatUserProfileUiState(
        user = User(
            id = "u1",
            username = "Alice",
            email = "alice@example.com",
            phoneNumber = PhoneNumber(countryCode = "+1", number = "5550100"),
        ),
        isOnline = true,
    )
    MessengerTheme {
        Surface {
            Column {
                ProfileHeader(state = state, onCallClick = { _, _, _ -> })
                ProfileInfoCard(state = state)
                MediaTabsRow(active = MediaTab.entries.first(), onChange = {})
                ProfileMediaGrid()
            }
        }
    }
}
