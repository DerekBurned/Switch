package com.example.messenger.presentation.screens.editprofile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.presentation.components.common.MessengerAvatar
import com.example.messenger.presentation.components.common.MessengerInputField
import com.example.messenger.presentation.components.common.PillButton
import com.example.messenger.presentation.components.common.RoundedCard
import com.example.messenger.presentation.components.common.SettingsRow
import com.example.messenger.presentation.components.common.model.PillButtonStyle
import com.example.messenger.presentation.featureflag.featureEnabled
import com.example.messenger.presentation.screens.settings.SettingsSubScaffold
import com.example.messenger.presentation.theme.Dimens
import com.example.messenger.presentation.theme.MessengerTheme
import com.example.messenger.presentation.theme.messengerTokens

@Composable
internal fun EditProfileScreenContent(
    state: EditProfileUiState,
    onBackClick: () -> Unit = {},
    onNameChange: (String) -> Unit = {},
    onUsernameChange: (String) -> Unit = {},
    onDobChange: (String) -> Unit = {},
    onSaveClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onChangeAccountClick: () -> Unit = {},
    onChangePhoneClick: () -> Unit = {},
) {
    val tokens = messengerTokens
    SettingsSubScaffold(title = "Edit profile", onBack = onBackClick) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MessengerAvatar(name = state.name, photoUrl = state.avatarUrl, size = Dimens.avatarLarge)
            if (featureEnabled(FeatureFlag.PROFILE_EDIT_CHANGE_PHOTO)) {
                TextButton(onClick = {}) {
                    Text("Change photo", color = tokens.accent, fontSize = 13.sp)
                }
            }
        }

        MessengerInputField(state.name, onNameChange, "Profile name")

        RoundedCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Phone number", color = tokens.textMuted, style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = state.phone.ifBlank { "(not set)" },
                        color = tokens.textPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (featureEnabled(FeatureFlag.PROFILE_CHANGE_PHONE)) {
                    TextButton(onClick = onChangePhoneClick) {
                        Text("Change", color = tokens.accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        MessengerInputField(state.username, onUsernameChange, "Username")
        MessengerInputField(state.dob, onDobChange, "Date of birth")

        if (featureEnabled(FeatureFlag.ACCOUNTS_SWITCH)) {
            RoundedCard {
                SettingsRow(title = "Switch account", onClick = onChangeAccountClick)
            }
        }

        state.error?.let { err ->
            Text(err.asString(), color = tokens.danger, fontSize = 13.sp)
        }

        Spacer(Modifier.height(4.dp))
        PillButton(
            text = "Confirm changes",
            onClick = onSaveClick,
            loading = state.isSaving,
            enabled = !state.isSaving,
        )
        PillButton(
            text = "Log out",
            onClick = onLogoutClick,
            style = PillButtonStyle.Danger,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun EditProfileScreenPreview() {
    MessengerTheme {
        EditProfileScreenContent(
            state = EditProfileUiState(
                name = "Alice",
                phone = "+1 555 0100",
                username = "alice",
                dob = "1995-04-15",
            ),
        )
    }
}
