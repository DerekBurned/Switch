package com.example.messenger.presentation.screens.chats

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.example.messenger.domain.model.Conversation
import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.theme.MessengerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatsTabContentTest {

    @get:Rule
    val compose = createComposeRule()

    private var clickedChat: List<String?>? = null

    private val conversation = Conversation(
        id = "c1",
        participantIds = listOf("me", "bob"),
        participantNames = listOf("Me", "Bob"),
        lastMessage = "see you",
        lastMessageTimestamp = 1_700_000_000_000L,
    )

    private fun render(state: ConversationsUiState) {
        compose.setContent {
            MessengerTheme {
                ChatsTabContent(
                    uiState = state,
                    topInset = 0.dp,
                    onRefresh = {},
                    onChatClick = { id, partnerId, name, avatar -> clickedChat = listOf(id, partnerId, name, avatar) },
                    onEditChats = {},
                    onMarkRead = {},
                    onMarkUnread = {},
                    onDeleteForMe = {},
                    onDeleteForEveryone = {},
                )
            }
        }
    }

    @Test
    fun `each row shows the partner's alias and the last message`() {
        render(
            ConversationsUiState(
                currentUserId = "me",
                conversations = listOf(conversation),
                aliases = mapOf("bob" to "Bobby"),
            ),
        )

        compose.onNodeWithText("Bobby").assertExists()
        compose.onNodeWithText("see you", substring = true).assertExists()
    }

    @Test
    fun `tapping a row calls onChatClick with the conversation and partner`() {
        render(ConversationsUiState(currentUserId = "me", conversations = listOf(conversation)))

        compose.onNodeWithText("Bob").performClick()

        assertThat(clickedChat).isEqualTo(listOf("c1", "bob", "Bob", null))
    }

    @Test
    fun `an error replaces the list`() {
        render(
            ConversationsUiState(
                currentUserId = "me",
                conversations = listOf(conversation),
                error = UiText.DynamicString("offline"),
            ),
        )

        compose.onNodeWithText("offline").assertExists()
        compose.onNodeWithText("see you", substring = true).assertDoesNotExist()
    }
}
