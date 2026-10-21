package com.example.messenger.presentation.screens.changeaccount

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.messenger.presentation.components.common.CardDivider
import com.example.messenger.presentation.components.common.RoundedCard
import com.example.messenger.presentation.components.list.AccountRow
import com.example.messenger.presentation.screens.settings.SettingsSubScaffold
import com.example.messenger.presentation.theme.MessengerTheme
import com.example.messenger.presentation.theme.messengerTokens

@Composable
internal fun ChangeAccountScreenContent(
    state: ChangeAccountUiState,
    onBackClick: () -> Unit = {},
    onSelect: (String) -> Unit = {},
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
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChangeAccountScreenPreview() {
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
