package com.example.messenger.data.local.obx

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class ObxConversationMapperTest {

    @Test
    fun `shows firestore lastMessage when it is newer than the stale locally-tracked latest`() {
        val obx = ObxConversation(
            uid = "c1",
            lastMessage = "new from partner",
            lastMessageSenderId = "partner",
            lastMessageTimestamp = 200,
            latestMessageText = "old local",
            latestMessageSenderId = "me",
            latestMessageTimestamp = 100,
        )

        val domain = obx.toDomain()

        assertThat(domain.lastMessage).isEqualTo("new from partner")
        assertThat(domain.lastMessageSenderId).isEqualTo("partner")
        assertThat(domain.lastMessageTimestamp).isEqualTo(200)
    }

    @Test
    fun `shows locally-tracked latest when it is newer than a stale firestore doc`() {
        val obx = ObxConversation(
            uid = "c1",
            lastMessage = "stale firestore doc",
            lastMessageSenderId = "partner",
            lastMessageTimestamp = 100,
            latestMessageText = "fresh local",
            latestMessageSenderId = "me",
            latestMessageTimestamp = 200,
        )

        val domain = obx.toDomain()

        assertThat(domain.lastMessage).isEqualTo("fresh local")
        assertThat(domain.lastMessageSenderId).isEqualTo("me")
        assertThat(domain.lastMessageTimestamp).isEqualTo(200)
    }

    @Test
    fun `falls back to firestore lastMessage when no locally-tracked latest exists`() {
        val obx = ObxConversation(
            uid = "c1",
            lastMessage = "only firestore",
            lastMessageSenderId = "partner",
            lastMessageTimestamp = 100,
            latestMessageText = null,
            latestMessageTimestamp = 0,
        )

        val domain = obx.toDomain()

        assertThat(domain.lastMessage).isEqualTo("only firestore")
        assertThat(domain.lastMessageTimestamp).isEqualTo(100)
    }
}
