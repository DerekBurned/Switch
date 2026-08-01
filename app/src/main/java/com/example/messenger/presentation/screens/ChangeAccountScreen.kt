package com.example.messenger.presentation.screens
import com.example.messenger.presentation.components.list.AccountRow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import com.example.messenger.presentation.components.common.CardDivider
import com.example.messenger.presentation.components.common.MessengerAvatar
import com.example.messenger.presentation.components.common.RoundedCard
import com.example.messenger.presentation.screens.settings.SettingsSubScaffold
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import com.example.messenger.presentation.state.AccountSummary
import com.example.messenger.presentation.state.ChangeAccountUiState
import com.example.messenger.presentation.viewmodel.ChangeAccountViewModel

@Composable
fun ChangeAccountScreen(
    viewModel: ChangeAccountViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onAddAccount: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChangeAccountScreenContent(
        state = state,
        onBackClick = onBackClick,
        onSelect = viewModel::selectAccount,
        onAddAccount = onAddAccount,
    )
}

@Composable
private fun ChangeAccountScreenContent(
    state: ChangeAccountUiState,
    onBackClick: () -> Unit = {},
    onSelect: (String) -> Unit = {},
    onAddAccount: () -> Unit = {},
) {
    val tokens = messengerTokens
    SettingsSubScaffold(title = "Accounts", onBack = onBackClick) {
        RoundedCard {
            state.accounts.forEachIndexed { index, account ->
                if (index > 0) CardDivider()
                AccountRow(
                    account = account,
                    isCurrent = account.id == state.currentAccountId,
                    onClick = { onSelect(account.id) },
                )
            }
        }
        RoundedCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddAccount)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(tokens.accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add account", tint = tokens.accent)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Add account",
                    color = tokens.textOnField,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ChangeAccountScreenPreview() {
    MessengerTheme {
        ChangeAccountScreenContent(
            state = ChangeAccountUiState(
                accounts = listOf(
                    AccountSummary("a1", "Alice", "+1 555 0100"),
                    AccountSummary("a2", "Bob Work", "+1 555 0200"),
                ),
                currentAccountId = "a1",
            ),
        )
    }
}
