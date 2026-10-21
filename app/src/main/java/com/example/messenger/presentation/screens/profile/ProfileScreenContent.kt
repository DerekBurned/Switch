package com.example.messenger.presentation.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.model.User
import com.example.messenger.presentation.components.common.CardDivider
import com.example.messenger.presentation.components.common.MessengerAvatar
import com.example.messenger.presentation.components.common.NavHeaderPill
import com.example.messenger.presentation.components.common.PillButton
import com.example.messenger.presentation.components.common.RoundedCard
import com.example.messenger.presentation.components.common.WallpaperBackground
import com.example.messenger.presentation.featureflag.featureEnabled
import com.example.messenger.presentation.theme.Dimens
import com.example.messenger.presentation.theme.MessengerTheme
import com.example.messenger.presentation.theme.messengerTokens

@Composable
internal fun ProfileScreenContent(
    uiState: ProfileUiState,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onAvatarClick: () -> Unit = {},
) {
    val tokens = messengerTokens
    val user = uiState.user
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        NavHeaderPill(title = "Profile", onBack = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenPadding)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            MessengerAvatar(
                name = user?.username.orEmpty(),
                photoUrl = uiState.photos.firstOrNull() ?: user?.avatarUrl,
                size = Dimens.avatarLarge,
                modifier = Modifier.clickable(onClick = onAvatarClick),
            )
            Spacer(modifier = Modifier.height(24.dp))
            RoundedCard {
                ProfileDisplayRow(
                    label = "Phone number",
                    value = user?.phoneNumber?.getFullNumber().orEmpty(),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            RoundedCard {
                ProfileDisplayRow(label = "Name", value = user?.username.orEmpty())
                CardDivider()
                ProfileDisplayRow(
                    label = "Username",
                    value = user?.username?.let { "@$it" }.orEmpty(),
                )
                CardDivider()
                ProfileDisplayRow(label = "Email", value = user?.email.orEmpty())
            }
            if (featureEnabled(FeatureFlag.PROFILE_EDIT)) {
                Spacer(modifier = Modifier.height(28.dp))
                PillButton(text = "Edit profile", onClick = onEditClick)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProfileScreenPreview() {
    MessengerTheme {
        WallpaperBackground {
            ProfileScreenContent(
                uiState = ProfileUiState(
                    user = User(
                        id = "preview-uid",
                        username = "Jane Doe",
                        email = "jane@example.com",
                    ),
                ),
                onBackClick = {},
                onEditClick = {},
            )
        }
    }
}
