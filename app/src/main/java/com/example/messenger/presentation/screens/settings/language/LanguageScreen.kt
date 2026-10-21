package com.example.messenger.presentation.screens.settings.language

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.messenger.presentation.components.common.CardDivider
import com.example.messenger.presentation.components.common.RoundedCard
import com.example.messenger.presentation.screens.settings.SettingsSubScaffold

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
