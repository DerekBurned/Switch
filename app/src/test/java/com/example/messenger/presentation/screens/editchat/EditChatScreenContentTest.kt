package com.example.messenger.presentation.screens.editchat

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.example.messenger.domain.model.Conversation
import com.example.messenger.presentation.theme.MessengerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditChatScreenContentTest {

    @get:Rule
    val compose = createComposeRule()

    private val toggled = mutableListOf<String>()
    private var deleteClicks = 0

    private val conversations = listOf(Conversation(id = "c1", participantNames = listOf("Alice")))

    private fun render(selectedIds: Set<String>) {
        compose.setContent {
            MessengerTheme {
                EditChatScreenContent(
                    state = EditChatUiState(conversations = conversations, selectedIds = selectedIds),
                    onToggle = { toggled += it },
                    onDelete = { deleteClicks++ },
                )
            }
        }
    }

    @Test
    fun `tapping a row calls onToggle with its id`() {
        render(selectedIds = emptySet())

        compose.onNodeWithText("Alice").performClick()

        assertThat(toggled).containsExactly("c1")
    }

    @Test
    fun `delete is disabled with nothing selected`() {
        render(selectedIds = emptySet())

        compose.onNodeWithText("Delete").assertIsNotEnabled()
    }

    @Test
    fun `delete calls onDelete`() {
        render(selectedIds = setOf("c1"))

        compose.onNodeWithText("Delete").performClick()

        assertThat(deleteClicks).isEqualTo(1)
    }
}
