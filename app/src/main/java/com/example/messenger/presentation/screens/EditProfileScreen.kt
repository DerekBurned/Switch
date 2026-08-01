package com.example.messenger.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.presentation.components.common.MessengerAvatar
import com.example.messenger.presentation.components.common.MessengerInputField
import com.example.messenger.presentation.components.common.PillButton
import com.example.messenger.presentation.components.common.PillButtonStyle
import com.example.messenger.presentation.components.common.RoundedCard
import com.example.messenger.presentation.components.common.SettingsRow
import com.example.messenger.presentation.screens.settings.SettingsSubScaffold
import com.example.messenger.presentation.base.ObserveAsEvents
import com.example.messenger.presentation.effect.EditProfileEffect
import com.example.messenger.presentation.intent.EditProfileIntent
import com.example.messenger.presentation.screens.ui.theme.Dimens
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import com.example.messenger.presentation.state.EditProfileUiState
import com.example.messenger.presentation.viewmodel.EditProfileViewModel

@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onSaved: () -> Unit = {},
    onLogout: () -> Unit = {},
    onChangeAccount: () -> Unit = {},
    onChangePhone: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.effect) { effect ->
        when (effect) {
            EditProfileEffect.Saved -> onSaved()
        }
    }

    EditProfileScreenContent(
        state = state,
        onBackClick = onBackClick,
        onNameChange = { viewModel.dispatch(EditProfileIntent.NameChange(it)) },
        onUsernameChange = { viewModel.dispatch(EditProfileIntent.UsernameChange(it)) },
        onDobChange = { viewModel.dispatch(EditProfileIntent.DobChange(it)) },
        onSaveClick = { viewModel.dispatch(EditProfileIntent.Save) },
        onLogoutClick = onLogout,
        onChangeAccountClick = onChangeAccount,
        onChangePhoneClick = onChangePhone,
    )
}

@Composable
private fun EditProfileScreenContent(
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
            TextButton(onClick = {}) {
                Text("Change photo", color = tokens.accent, fontSize = 13.sp)
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
                        color = tokens.textOnField,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                TextButton(onClick = onChangePhoneClick) {
                    Text("Change", color = tokens.accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        MessengerInputField(state.username, onUsernameChange, "Username")
        MessengerInputField(state.dob, onDobChange, "Date of birth")

        RoundedCard {
            SettingsRow(title = "Switch account", onClick = onChangeAccountClick)
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
fun EditProfileScreenPreview() {
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
