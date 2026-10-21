package com.example.messenger.data.repository

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.example.messenger.data.local.obx.ObxMessage
import com.example.messenger.data.local.obx.ObxSyncQueueItem
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.data.remote.firebase.FirestoreService
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.SyncStatus
import com.example.messenger.domain.model.sync.SyncAction
import com.example.messenger.domain.model.sync.SyncEntityType
import com.example.messenger.testutil.AndroidLogExtension
import com.example.messenger.testutil.ObjectBoxExtension
import com.example.messenger.testutil.fakes.FakeConversationRepository
import com.example.messenger.testutil.fakes.FakeUserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class SyncRepositoryImplIntegrationTest {

    @JvmField
    @RegisterExtension
    val log = AndroidLogExtension()

    @JvmField
    @RegisterExtension
    val obx = ObjectBoxExtension()

    private val firestore = mockk<FirestoreService>()
    private val auth = mockk<FirebaseAuthService> { every { getCurrentUserId() } returns "me" }
    private val conversationRepository = FakeConversationRepository()

    private val syncRepository by lazy {
        SyncRepositoryImpl(
            syncQueueBox = obx.box(),
            messageBox = obx.box(),
            firestoreService = firestore,
            conversationRepository = conversationRepository,
            userRepository = FakeUserRepository(),
        )
    }

    private val messageRepository by lazy {
        MessageRepositoryImpl(
            messageBox = obx.box(),
            conversationBox = obx.box(),
            syncQueueBox = obx.box(),
            firestoreService = firestore,
            authService = auth,
        )
    }

    private fun seedMessage(id: String, status: MessageStatus = MessageStatus.FAILED) {
        obx.box<ObxMessage>().put(
            ObxMessage(uid = id, conversationId = "c1", senderId = "me", text = "hi", status = status.name),
        )
    }

    private fun queue(id: String, action: String = SyncAction.SEND, timestamp: Long = 1, retryCount: Int = 0) {
        obx.box<ObxSyncQueueItem>().put(
            ObxSyncQueueItem(
                entityType = SyncEntityType.MESSAGE,
                entityId = id,
                action = action,
                timestamp = timestamp,
                retryCount = retryCount,
            ),
        )
    }

    private fun queued(): List<ObxSyncQueueItem> = obx.box<ObxSyncQueueItem>().all

    private fun status(id: String): String = obx.box<ObxMessage>().all.single { it.uid == id }.status

    @Test
    fun `queued send is resent and the message flips to SENT`() = runTest {
        seedMessage("m1")
        queue("m1")
        coEvery { firestore.sendMessage(any(), any()) } returns Result.success(Unit)

        syncRepository.processSyncQueue()

        assertThat(queued()).isEmpty()
        assertThat(status("m1")).isEqualTo(MessageStatus.SENT.name)
        coVerify { firestore.sendMessage(match { it.id == "m1" && it.status == MessageStatus.SENT }, any()) }
    }

    @Test
    fun `failed resend increments retryCount and keeps the item`() = runTest {
        seedMessage("m1")
        queue("m1")
        coEvery { firestore.sendMessage(any(), any()) } returns Result.failure(Exception("offline"))

        syncRepository.processSyncQueue()

        assertThat(queued().single().retryCount).isEqualTo(1)
        assertThat(status("m1")).isEqualTo(MessageStatus.FAILED.name)
    }

    @Test
    fun `fifth failed resend drops the item and leaves the message FAILED`() = runTest {
        seedMessage("m1")
        queue("m1", retryCount = 4)
        coEvery { firestore.sendMessage(any(), any()) } returns Result.failure(Exception("offline"))

        syncRepository.processSyncQueue()

        assertThat(queued()).isEmpty()
        assertThat(status("m1")).isEqualTo(MessageStatus.FAILED.name)
    }

    @Test
    fun `queued item for a missing message is dropped`() = runTest {
        queue("gone")

        syncRepository.processSyncQueue()

        assertThat(queued()).isEmpty()
        coVerify(exactly = 0) { firestore.sendMessage(any(), any()) }
    }

    @Test
    fun `queued DELETE_FOR_ME calls deleteMessageForMe`() = runTest {
        seedMessage("m1", status = MessageStatus.SENT)
        queue("m1", action = SyncAction.DELETE_FOR_ME)
        coEvery { firestore.deleteMessageForMe(any()) } returns Result.success(Unit)

        syncRepository.processSyncQueue()

        assertThat(queued()).isEmpty()
        coVerify { firestore.deleteMessageForMe(match { it.id == "m1" }) }
        coVerify(exactly = 0) { firestore.deleteMessage(any()) }
    }

    @Test
    fun `items are processed oldest first`() = runTest {
        seedMessage("m1")
        seedMessage("m2")
        queue("m1", timestamp = 2)
        queue("m2", timestamp = 1)
        coEvery { firestore.sendMessage(any(), any()) } returns Result.success(Unit)

        syncRepository.processSyncQueue()

        coVerifyOrder {
            firestore.sendMessage(match { it.id == "m2" }, any())
            firestore.sendMessage(match { it.id == "m1" }, any())
        }
    }

    @Test
    fun `syncAllData still processes the queue when conversation sync throws`() = runTest {
        conversationRepository.syncError = IllegalStateException("boom")
        seedMessage("m1")
        queue("m1")
        coEvery { firestore.sendMessage(any(), any()) } returns Result.success(Unit)

        val result = syncRepository.syncAllData()

        assertThat(result.isSuccess).isTrue()
        assertThat(queued()).isEmpty()
        assertThat(syncRepository.observeSyncStatus().first()).isEqualTo(SyncStatus.SYNCED)
    }

    @Test
    fun `a message that failed offline is delivered by the next sync`() = runTest {
        coEvery { firestore.sendMessage(any(), any()) } returnsMany listOf(
            Result.failure(Exception("offline")),
            Result.success(Unit),
        )
        val message = Message.Text(
            id = "m1",
            conversationId = "c1",
            senderId = "me",
            timestamp = 1,
            status = MessageStatus.SENDING,
            text = "hi",
        )

        messageRepository.sendMessage(message)
        assertThat(status("m1")).isEqualTo(MessageStatus.FAILED.name)

        syncRepository.processSyncQueue()

        assertThat(queued()).isEmpty()
        val delivered = messageRepository.getMessagesStream("c1").first().single()
        assertThat(delivered.status).isEqualTo(MessageStatus.SENT)
    }
}
