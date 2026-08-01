package com.example.messenger.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.messenger.presentation.components.common.CardDivider
import com.example.messenger.presentation.components.common.RoundedCard
import com.example.messenger.presentation.screens.ui.theme.Dimens
import com.example.messenger.presentation.screens.ui.theme.messengerTokens

@Composable
fun LanguageScreen(onBack: () -> Unit) {
    val languages = listOf("English", "Polski", "Русский", "Українська", "Español", "Deutsch")
    var selected by rememberSaveable { mutableIntStateOf(0) }

    SettingsSubScaffold(title = "Language", onBack = onBack) {
        RoundedCard {
            languages.forEachIndexed { index, language ->
                if (index > 0) CardDivider()
                LanguageRow(
                    name = language,
                    selected = index == selected,
                    onClick = { selected = index },
                )
            }
        }
    }
}

@Composable
private fun LanguageRow(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = messengerTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.rowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            color = tokens.textOnField,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = tokens.accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
