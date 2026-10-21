package com.example.messenger.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.BuildConfig
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.model.User
import com.example.messenger.presentation.components.call.LocalCallBarInset
import com.example.messenger.presentation.components.common.CardDivider
import com.example.messenger.presentation.components.common.LocalClipBarInset
import com.example.messenger.presentation.components.common.MessengerAvatar
import com.example.messenger.presentation.components.common.RoundedCard
import com.example.messenger.presentation.components.common.SettingsIconTile
import com.example.messenger.presentation.components.common.SettingsRow
import com.example.messenger.presentation.components.common.WallpaperBackground
import com.example.messenger.presentation.featureflag.featureEnabled
import com.example.messenger.presentation.theme.LocalTabBarPosition
import com.example.messenger.presentation.theme.MessengerTheme
import com.example.messenger.presentation.theme.messengerTokens
import com.example.messenger.presentation.theme.model.BarPosition

@Composable
fun SettingsScreenContent(
    user: User?,
    photoUrl: String? = null,
    onProfileClick: () -> Unit = {},
    onSwitchAccountClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onAppearanceClick: () -> Unit = {},
    onSecurityClick: () -> Unit = {},
    onDataStorageClick: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
) {
    val tokens = messengerTokens
    val barAtBottom = LocalTabBarPosition.current == BarPosition.BOTTOM
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(bottom = 40.dp + if (barAtBottom) 72.dp else 0.dp),
    ) {
        Spacer(
            modifier = Modifier.height(
                (if (barAtBottom) 0.dp else 72.dp) + LocalCallBarInset.current + LocalClipBarInset.current,
            ),
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(tokens.cardFill)
                .border(1.dp, tokens.panelBorder, RoundedCornerShape(26.dp))
                .clickable { onProfileClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MessengerAvatar(
                name = user?.username.orEmpty(),
                photoUrl = photoUrl,
                size = 62.dp,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user?.username ?: "User",
                    color = tokens.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "@${user?.username ?: "user"}",
                    color = tokens.accent,
                    fontSize = 14.sp,
                )
                val contact = user?.email ?: user?.phoneNumber?.getFullNumber() ?: ""
                if (contact.isNotBlank()) {
                    Text(
                        text = contact,
                        color = tokens.textMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = tokens.textMuted,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (featureEnabled(FeatureFlag.ACCOUNTS_SWITCH)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(tokens.pillFill)
                    .border(1.dp, tokens.panelBorder, RoundedCornerShape(18.dp))
                    .clickable { onSwitchAccountClick() }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = null,
                    tint = tokens.accent,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Switch account",
                    color = tokens.textPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.UnfoldMore,
                    contentDescription = null,
                    tint = tokens.textMuted,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        val securityEnabled = featureEnabled(FeatureFlag.SETTINGS_SECURITY)
        val notificationsEnabled = featureEnabled(FeatureFlag.SETTINGS_NOTIFICATION_CONTROLS)
        val dataStorageEnabled = featureEnabled(FeatureFlag.SETTINGS_DATA_STORAGE)
        val languageEnabled = featureEnabled(FeatureFlag.SETTINGS_LANGUAGE)
        val rows = buildList<@Composable () -> Unit> {
            add {
                SettingsRow(
                    title = "Privacy",
                    subtitle = "Last seen, read receipts, blocked",
                    onClick = onPrivacyClick,
                    leading = { SettingsIconTile(Icons.Filled.Lock, Color(0xFF3C85B0)) },
                )
            }
            if (securityEnabled) add {
                SettingsRow(
                    title = "Security",
                    subtitle = "Sessions, app lock, sign-in",
                    onClick = onSecurityClick,
                    leading = { SettingsIconTile(Icons.Filled.Shield, Color(0xFF2E6F93)) },
                )
            }
            if (notificationsEnabled) add {
                SettingsRow(
                    title = "Notifications & Sounds",
                    subtitle = "Sound, vibration, preview",
                    onClick = onNotificationsClick,
                    leading = { SettingsIconTile(Icons.Filled.Notifications, Color(0xFFFF9F0A)) },
                )
            }
            add {
                SettingsRow(
                    title = "Appearance",
                    subtitle = "Theme, text size, wallpaper",
                    onClick = onAppearanceClick,
                    leading = { SettingsIconTile(Icons.Filled.Palette, Color(0xFF5AA9C9)) },
                )
            }
            if (dataStorageEnabled) add {
                SettingsRow(
                    title = "Data & Storage",
                    subtitle = "Storage, auto-download, network",
                    onClick = onDataStorageClick,
                    leading = { SettingsIconTile(Icons.Filled.DataUsage, Color(0xFF30C44A)) },
                )
            }
            if (languageEnabled) add {
                SettingsRow(
                    title = "Language",
                    subtitle = "English",
                    onClick = onLanguageClick,
                    leading = { SettingsIconTile(Icons.Filled.Language, Color(0xFFEC407A)) },
                )
            }
        }
        RoundedCard {
            rows.forEachIndexed { index, row ->
                if (index > 0) CardDivider()
                row()
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(tokens.cardFill)
                .border(1.dp, tokens.panelBorder, RoundedCornerShape(22.dp))
                .clickable { onLogoutClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIconTile(Icons.AutoMirrored.Filled.Logout, tokens.danger)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Log out",
                color = tokens.danger,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Text(
            text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            color = tokens.textMuted.copy(alpha = 0.6f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        )

        val uriHandler = LocalUriHandler.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClickLabel = "View license") { runCatching { uriHandler.openUri(LICENSE_URL) } }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "© 2025-2026 Derek Burned and contributors",
                color = tokens.textMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Free software: you may share and modify it under the GNU AGPL version 3. It comes with no warranty. Tap to view the license.",
                color = tokens.textMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val LICENSE_URL = "https://www.gnu.org/licenses/agpl-3.0.html"

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SettingsScreenPreview() {
    MessengerTheme {
        WallpaperBackground {
            SettingsScreenContent(
                user = User(
                    id = "preview-uid",
                    username = "Jane Doe",
                    email = "jane@example.com",
                ),
            )
        }
    }
}
