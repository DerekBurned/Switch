package com.example.messenger.presentation.contrast

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.messenger.presentation.components.list.AccountRow
import com.example.messenger.presentation.components.list.ContactRow
import com.example.messenger.presentation.components.list.ConversationListItem
import com.example.messenger.presentation.components.list.EditChatRow
import com.example.messenger.presentation.components.list.ForwardConversationRow
import com.example.messenger.presentation.components.list.SearchUserItem
import com.example.messenger.presentation.components.list.SwipeableConversationRow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListComponentContrastTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun accountRow() = compose.assertReadableInBothThemes("AccountRow") {
        AccountRow(account = Fixtures.account, isCurrent = true, onClick = {})
    }

    @Test
    fun contactRow() = compose.assertReadableInBothThemes("ContactRow") {
        ContactRow(user = Fixtures.user, onClick = {})
    }

    @Test
    fun conversationListItem() = compose.assertReadableInBothThemes("ConversationListItem") {
        ConversationListItem(
            conversation = Fixtures.conversation,
            partnerId = "u2",
            displayName = "Bob Martin",
            presence = Fixtures.presence,
            currentUserId = Fixtures.CURRENT_USER_ID,
            onClick = {},
        )
    }

    @Test
    fun editChatRow() = compose.assertReadableInBothThemes("EditChatRow") {
        EditChatRow(conversation = Fixtures.conversation, selected = true, onClick = {})
    }

    @Test
    fun forwardConversationRow() = compose.assertReadableInBothThemes("ForwardConversationRow") {
        ForwardConversationRow(
            conversation = Fixtures.conversation,
            currentUserId = Fixtures.CURRENT_USER_ID,
            onClick = {},
        )
    }

    @Test
    fun searchUserItem() = compose.assertReadableInBothThemes("SearchUserItem") {
        SearchUserItem(user = Fixtures.user, onClick = {})
    }

    @Test
    fun swipeableConversationRow() = compose.assertReadableInBothThemes("SwipeableConversationRow") {
        SwipeableConversationRow(
            displayName = "Bob Martin",
            isOneOnOne = true,
            hasUnread = true,
            onClick = {},
            onEdit = {},
            onMarkRead = {},
            onMarkUnread = {},
            onDeleteForMe = {},
            onDeleteForEveryone = {},
        ) { rowOnClick ->
            ConversationListItem(
                conversation = Fixtures.conversation,
                partnerId = "u2",
                displayName = "Bob Martin",
                presence = Fixtures.presence,
                currentUserId = Fixtures.CURRENT_USER_ID,
                onClick = rowOnClick,
            )
        }
    }
}
