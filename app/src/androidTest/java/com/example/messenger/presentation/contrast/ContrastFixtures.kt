package com.example.messenger.presentation.contrast

import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.model.User
import com.example.messenger.domain.model.presence.PresenceState
import com.example.messenger.domain.model.presence.UserPresence
import com.example.messenger.presentation.screens.changeaccount.AccountSummary

internal object Fixtures {

    const val CURRENT_USER_ID = "me"

    val user = User(
        id = "u1",
        username = "Alice Johnson",
        usernameLower = "alice johnson",
        email = "alice@example.com",
        isOnline = true,
        lastSeen = 1_700_000_000_000L,
    )

    val conversation = Conversation(
        id = "c1",
        participantIds = listOf(CURRENT_USER_ID, "u2"),
        participantNames = listOf("Me", "Bob Martin"),
        lastMessage = "See you tomorrow",
        lastMessageSenderId = "u2",
        lastMessageTimestamp = 1_700_000_000_000L,
        unreadCount = 3,
    )

    val account = AccountSummary(
        id = "a1",
        name = "Alice Johnson",
        phone = "+1 555 0100",
    )

    val presence = UserPresence(
        state = PresenceState.ONLINE,
        lastSeen = 1_700_000_000_000L,
    )
}
