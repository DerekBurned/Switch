package com.example.messenger.data.repository

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.example.messenger.data.local.obx.ObxConversation
import com.example.messenger.data.local.obx.ObxMessage
import com.example.messenger.data.local.obx.mappers.toObx
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.data.remote.firebase.FirestoreService
import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.model.User
import com.example.messenger.testutil.AndroidLogExtension
import com.example.messenger.testutil.ObjectBoxExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ConversationRepositoryImplIntegrationTest {

    @JvmField
    @RegisterExtension
    val log = AndroidLogExtension()

    @JvmField
    @RegisterExtension
    val obx = ObjectBoxExtension()

    private val firestore = mockk<FirestoreService>()
    private val auth = mockk<FirebaseAuthService> { every { getCurrentUserId() } returns "me" }

    private val repository by lazy {
        ConversationRepositoryImpl(
            firestoreService = firestore,
            conversationBox = obx.box(),
            messageBox = obx.box(),
            authService = auth,
        )
    }

    private fun seedConversation(
        id: String,
        lastMessageTimestamp: Long = 0,
        unreadCount: Int = 0,
        latestMessageTimestamp: Long = 0,
    ) {
        obx.box<ObxConversation>().put(
            Conversation(
                id = id,
                participantIds = listOf("me", "bob"),
                lastMessageTimestamp = lastMessageTimestamp,
                unreadCount = unreadCount,
            ).toObx().apply { this.latestMessageTimestamp = latestMessageTimestamp },
        )
    }

    private fun seedMessage(
        id: String,
        conversationId: String = "c1",
        senderId: String = "bob",
        timestamp: Long = 1,
        isRead: Boolean = false,
    ) {
        obx.box<ObxMessage>().put(
            ObxMessage(
                uid = id,
                conversationId = conversationId,
                senderId = senderId,
                timestamp = timestamp,
                isRead = isRead,
            ),
        )
    }

    private fun message(id: String): ObxMessage = obx.box<ObxMessage>().all.single { it.uid == id }

    private fun conversation(id: String): ObxConversation =
        obx.box<ObxConversation>().all.single { it.uid == id }

    private fun conversationIds(): List<String> = obx.box<ObxConversation>().all.map { it.uid }

    private fun messageIds(): List<String> = obx.box<ObxMessage>().all.map { it.uid }

    @Test
    fun `marking read zeroes unread and marks only partner messages read`() = runTest {
        seedConversation("c1", unreadCount = 3)
        seedMessage("m1", senderId = "bob")
        seedMessage("m2", senderId = "me")
        seedMessage("m3", conversationId = "c2", senderId = "bob")
        coEvery { firestore.resetUnreadCount("c1", "me") } returns Result.success(Unit)

        val result = repository.markConversationAsRead("c1")

        assertThat(result.isSuccess).isTrue()
        assertThat(conversation("c1").unreadCount).isEqualTo(0)
        assertThat(message("m1").isRead).isTrue()
        assertThat(message("m2").isRead).isFalse()
        assertThat(message("m3").isRead).isFalse()
        coVerify { firestore.resetUnreadCount("c1", "me") }
    }

    @Test
    fun `marking unread flips only the latest partner message and sets unread to at least 1`() = runTest {
        seedConversation("c1", unreadCount = 0)
        seedMessage("m1", senderId = "bob", timestamp = 1, isRead = true)
        seedMessage("m2", senderId = "bob", timestamp = 2, isRead = true)
        seedMessage("m3", senderId = "me", timestamp = 3, isRead = true)

        repository.markConversationAsUnread("c1")

        assertThat(message("m1").isRead).isTrue()
        assertThat(message("m2").isRead).isFalse()
        assertThat(message("m3").isRead).isTrue()
        assertThat(conversation("c1").unreadCount).isEqualTo(1)
    }

    @Test
    fun `sync prunes conversations the server no longer has, with their messages`() = runTest {
        seedConversation("c1")
        seedConversation("c2")
        seedMessage("m1", conversationId = "c1")
        seedMessage("m2", conversationId = "c2")
        coEvery { firestore.fetchAllConversationsOnce("me") } returns Result.success(
            listOf(Conversation(id = "c1", participantIds = listOf("me", "bob"))),
        )

        repository.syncConversations()

        assertThat(conversationIds()).containsExactly("c1")
        assertThat(messageIds()).containsExactly("m1")
    }

    @Test
    fun `sync keeps everything when the server fetch fails`() = runTest {
        seedConversation("c1")
        seedConversation("c2")
        coEvery { firestore.fetchAllConversationsOnce("me") } returns Result.failure(Exception("offline"))

        repository.syncConversations()

        assertThat(conversationIds()).containsExactlyInAnyOrder("c1", "c2")
    }

    @Test
    fun `creating reuses an existing remote conversation`() = runTest {
        coEvery { firestore.findExistingConversation(listOf("bob"), "me") } returns Result.success(
            Conversation(id = "c9", participantIds = listOf("me", "bob")),
        )

        val result = repository.createConversation(listOf("bob"))

        assertThat(result.getOrNull()?.id).isEqualTo("c9")
        assertThat(conversationIds()).containsExactly("c9")
        coVerify(exactly = 0) { firestore.createConversation(any()) }
    }

    @Test
    fun `creating with a deleted account fails and stores nothing`() = runTest {
        coEvery { firestore.findExistingConversation(listOf("bob"), "me") } returns Result.success(null)
        coEvery { firestore.getUserProfile("bob") } returns Result.success(User(id = "bob", deleted = true))

        val result = repository.createConversation(listOf("bob"))

        assertThat(result.isFailure).isTrue()
        assertThat(conversationIds()).isEmpty()
        coVerify(exactly = 0) { firestore.createConversation(any()) }
    }

    @Test
    fun `clearing for me sends the latest timestamp as cutoff and prunes locally`() = runTest {
        seedConversation("c1", lastMessageTimestamp = 10, latestMessageTimestamp = 20)
        seedMessage("m1", conversationId = "c1")
        coEvery { firestore.clearConversationForMe("c1", "me", 20) } returns Result.success(Unit)

        val result = repository.clearConversationForMe("c1")

        assertThat(result.isSuccess).isTrue()
        assertThat(conversationIds()).isEmpty()
        assertThat(messageIds()).isEmpty()
        coVerify { firestore.clearConversationForMe("c1", "me", 20) }
    }

    @Test
    fun `getAllConversations emits local rows newest first`() = runTest {
        every { auth.getCurrentUserId() } returns null
        seedConversation("c1", lastMessageTimestamp = 1)
        seedConversation("c2", lastMessageTimestamp = 2)

        repository.getAllConversations().test {
            assertThat(awaitItem().map { it.id }).containsExactly("c2", "c1")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
