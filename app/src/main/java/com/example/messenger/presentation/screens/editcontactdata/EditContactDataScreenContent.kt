package com.example.messenger.presentation.screens.editcontactdata

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.presentation.components.call.CallAwareTopBar
import com.example.messenger.presentation.components.common.MessengerInputField
import com.example.messenger.presentation.components.common.NavHeaderPill
import com.example.messenger.presentation.components.common.PillButton
import com.example.messenger.presentation.components.common.WallpaperBackground
import com.example.messenger.presentation.components.common.model.PillButtonStyle
import com.example.messenger.presentation.featureflag.featureEnabled
import com.example.messenger.presentation.theme.LightGray
import com.example.messenger.presentation.theme.MessengerTheme
import com.example.messenger.presentation.theme.PrimaryBlue
import com.example.messenger.presentation.theme.messengerTokens

@Composable
internal fun EditContactDataScreenContent(
    state: EditContactDataUiState,
    onBackClick: () -> Unit = {},
    onNameChange: (String) -> Unit = {},
    onSave: () -> Unit = {},
    onShowDeleteConfirm: () -> Unit = {},
    onDismissDeleteConfirm: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
) {
    val tokens = messengerTokens
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
    ) {
        CallAwareTopBar {
            NavHeaderPill(title = "Edit Contact", onBack = onBackClick)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(84.dp).clip(CircleShape).background(LightGray),
                    contentAlignment = Alignment.Center,
                ) {
                    val initial = state.name.ifBlank { state.username }.take(1)
                    if (initial.isNotBlank()) {
                        Text(initial, color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(40.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (state.username.isNotBlank()) {
                    Text("@${state.username}", color = tokens.textPrimary.copy(alpha = 0.6f), fontSize = 13.sp)
                }
                if (featureEnabled(FeatureFlag.PROFILE_EDIT_CHANGE_PHOTO)) {
                    TextButton(onClick = {}) {
                        Text("Change photo", color = tokens.accent, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            MessengerInputField(
                value = state.name,
                onValueChange = onNameChange,
                placeholder = "Profile name",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )

            Spacer(Modifier.height(28.dp))

            PillButton(
                text = "Confirm changes",
                onClick = onSave,
                enabled = !state.isSaving,
                loading = state.isSaving,
            )

            Spacer(Modifier.height(10.dp))

            PillButton(
                text = "Delete contact",
                onClick = onShowDeleteConfirm,
                style = PillButtonStyle.Danger,
            )
        }
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = onDismissDeleteConfirm,
            title = { Text("Delete contact?") },
            text = { Text("This will also delete the chat.", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = onDismissDeleteConfirm) { Text("Cancel") } },
            shape = RoundedCornerShape(16.dp),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun EditContactDataScreenPreview() {
    MessengerTheme {
        WallpaperBackground {
            EditContactDataScreenContent(state = EditContactDataUiState(name = "Alice", initialName = "Alice"))
        }
    }
}
