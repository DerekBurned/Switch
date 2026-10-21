package com.example.messenger.data.repository

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.example.messenger.data.local.obx.ObxConversation
import com.example.messenger.data.local.obx.ObxMessage
import com.example.messenger.data.local.obx.ObxSyncQueueItem
import com.example.messenger.data.local.obx.mappers.toObx
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.data.remote.firebase.FirestoreService
import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.sync.SyncAction
import com.example.messenger.domain.model.sync.SyncEntityType
import com.example.messenger.testutil.AndroidLogExtension
import com.example.messenger.testutil.ObjectBoxExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class MessageRepositoryImplIntegrationTest {

    @JvmField
    @RegisterExtension
    val log = AndroidLogExtension()

    @JvmField
    @RegisterExtension
    val obx = ObjectBoxExtension()

    private val firestore = mockk<FirestoreService>()
    private val auth = mockk<FirebaseAuthService> { every { getCurrentUserId() } returns "me" }

    private val repository by lazy {
        MessageRepositoryImpl(
            messageBox = obx.box(),
            conversationBox = obx.box(),
            syncQueueBox = obx.box(),
            firestoreService = firestore,
            authService = auth,
        )
    }

    private fun seedConversation(clearedAtForMe: Long = 0) {
        obx.box<ObxConversation>().put(
            Conversation(id = "c1", participantIds = listOf("me", "bob")).toObx()
                .apply { this.clearedAtForMe = clearedAtForMe },
        )
    }

    private fun text(
        id: String,
        senderId: String = "me",
        timestamp: Long = 1,
        body: String = "hi",
    ) = Message.Text(
        id = id,
        conversationId = "c1",
        senderId = senderId,
        timestamp = timestamp,
        status = MessageStatus.SENDING,
        text = body,
    )

    private fun storedRow(id: String): ObxMessage = obx.box<ObxMessage>().all.single { it.uid == id }

    private fun conversationRow(): ObxConversation = obx.box<ObxConversation>().all.single()

    @Test
    fun `sent message is stored and appears in the conversation stream`() = runTest {
        coEvery { firestore.sendMessage(any(), any()) } returns Result.success(Unit)

        repository.sendMessage(text("m1"))

        repository.getMessagesStream("c1").test {
            val messages = awaitItem()
            assertThat(messages.map { it.id }).containsExactly("m1")
            assertThat(messages.single().status).isEqualTo(MessageStatus.SENDING)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { firestore.sendMessage(match { it.id == "m1" && it.status == MessageStatus.SENT }, any()) }
    }

    @Test
    fun `failed send marks the message FAILED and queues a SEND retry`() = runTest {
        coEvery { firestore.sendMessage(any(), any()) } returns Result.failure(Exception("offline"))

        val result = repository.sendMessage(text("m1"))

        assertThat(result.isSuccess).isTrue()
        assertThat(storedRow("m1").status).isEqualTo("FAILED")
        val queued = obx.box<ObxSyncQueueItem>().all.single()
        assertThat(queued.entityId).isEqualTo("m1")
        assertThat(queued.entityType).isEqualTo(SyncEntityType.MESSAGE)
        assertThat(queued.action).isEqualTo(SyncAction.SEND)
    }

    @Test
    fun `sending updates the conversation preview`() = runTest {
        seedConversation()
        coEvery { firestore.sendMessage(any(), any()) } returns Result.success(Unit)

        repository.sendMessage(text("m1", timestamp = 42, body = "hello"))

        val conversation = conversationRow()
        assertThat(conversation.lastMessage).isEqualTo("hello")
        assertThat(conversation.latestMessageText).isEqualTo("hello")
        assertThat(conversation.lastMessageSenderId).isEqualTo("me")
        assertThat(conversation.lastMessageTimestamp).isEqualTo(42L)
    }

    @Test
    fun `three photos without a caption preview as 3 photos`() = runTest {
        seedConversation()
        coEvery { firestore.sendMessage(any(), any()) } returns Result.success(Unit)
        val media = Message.Media(
            id = "m2",
            conversationId = "c1",
            senderId = "me",
            items = List(3) { MediaItem(id = "i$it", kind = MediaItem.IMAGE) },
        )

        repository.sendMessage(media)

        assertThat(conversationRow().lastMessage).isEqualTo("3 photos")
    }

    @Test
    fun `a photo and a video without a caption preview as 2 media`() = runTest {
        seedConversation()
        coEvery { firestore.sendMessage(any(), any()) } returns Result.success(Unit)
        val media = Message.Media(
            id = "m3",
            conversationId = "c1",
            senderId = "me",
            items = listOf(
                MediaItem(id = "i1", kind = MediaItem.IMAGE),
                MediaItem(id = "i2", kind = MediaItem.VIDEO),
            ),
        )

        repository.sendMessage(media)

        assertThat(conversationRow().lastMessage).isEqualTo("2 media")
    }

    @Test
    fun `deleting someone else's message for everyone is refused and keeps it`() = runTest {
        val incoming = text("m1", senderId = "bob")
        repository.persistIncomingMessage(incoming)

        val result = repository.deleteMessageForEveryone(incoming)

        assertThat(result.exceptionOrNull()).isNotNull().isInstanceOf(SecurityException::class)
        assertThat(storedRow("m1").deleted).isFalse()
        coVerify(exactly = 0) { firestore.deleteMessage(any()) }
    }

    @Test
    fun `deleting for me hides the message even when Firestore fails`() = runTest {
        val incoming = text("m1", senderId = "bob")
        repository.persistIncomingMessage(incoming)
        coEvery { firestore.deleteMessageForMe(any()) } returns Result.failure(Exception("offline"))

        val result = repository.deleteMessageForMe(incoming)

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.getMessagesStream("c1").first()).isEmpty()
        val queued = obx.box<ObxSyncQueueItem>().all.single()
        assertThat(queued.entityId).isEqualTo("m1")
        assertThat(queued.action).isEqualTo(SyncAction.DELETE_FOR_ME)
    }

    @Test
    fun `incoming duplicate is stored once`() = runTest {
        repository.persistIncomingMessage(text("m1", senderId = "bob"))
        repository.persistIncomingMessage(text("m1", senderId = "bob", body = "edited"))

        assertThat(obx.box<ObxMessage>().count()).isEqualTo(1L)
        assertThat(storedRow("m1").text).isEqualTo("hi")
    }

    @Test
    fun `re-fetched history keeps local read, deleted and played flags`() = runTest {
        obx.box<ObxMessage>().put(
            ObxMessage(
                uid = "m1",
                conversationId = "c1",
                senderId = "bob",
                text = "old",
                status = MessageStatus.READ.name,
                isRead = true,
                deleted = true,
                clipPlayed = true,
            ),
        )
        coEvery { firestore.fetchRecentMessagesOnce("c1", 30, 0) } returns Result.success(
            listOf(
                Message.Text(
                    id = "m1",
                    conversationId = "c1",
                    senderId = "bob",
                    status = MessageStatus.SENT,
                    text = "new",
                ),
            ),
        )

        val result = repository.backfillRecentMessages("c1")

        assertThat(result).isEqualTo(Result.success(1))
        assertThat(obx.box<ObxMessage>().count()).isEqualTo(1L)
        val row = storedRow("m1")
        assertThat(row.text).isEqualTo("new")
        assertThat(row.isRead).isTrue()
        assertThat(row.deleted).isTrue()
        assertThat(row.clipPlayed).isTrue()
        assertThat(row.status).isEqualTo(MessageStatus.READ.name)
    }

    @Test
    fun `loading older messages passes the clear-for-me cutoff`() = runTest {
        seedConversation(clearedAtForMe = 500)
        repository.persistIncomingMessage(text("m1", senderId = "bob", timestamp = 600))
        coEvery { firestore.fetchOlderMessages("c1", "m1", 100, 500) } returns Result.success(emptyList())

        val result = repository.loadOlderMessages("c1")

        assertThat(result).isEqualTo(Result.success(0))
        coVerify { firestore.fetchOlderMessages("c1", "m1", 100, 500) }
    }

    @Test
    fun `loading older with no local messages returns zero without calling Firestore`() = runTest {
        val result = repository.loadOlderMessages("c1")

        assertThat(result).isEqualTo(Result.success(0))
        coVerify(exactly = 0) { firestore.fetchOlderMessages(any(), any(), any(), any()) }
    }
}
