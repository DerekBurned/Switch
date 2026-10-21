package com.example.messenger.presentation.contrast

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.messenger.presentation.components.common.RoundedCard
import com.example.messenger.presentation.screens.settings.datastorage.DataStorageScreen
import com.example.messenger.presentation.screens.settings.language.LanguageRow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenContrastTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun languageRows() = compose.assertReadableInBothThemes("LanguageRow") {
        RoundedCard {
            LanguageRow(name = "English", selected = true, onClick = {})
            LanguageRow(name = "Deutsch", selected = false, onClick = {})
        }
    }

    @Test
    fun dataStorageCard() = compose.assertReadableInBothThemes(
        component = "DataStorageScreen",
        texts = setOf("Storage", "5 GB of 128 GB"),
    ) {
        DataStorageScreen(onBack = {})
    }
}
