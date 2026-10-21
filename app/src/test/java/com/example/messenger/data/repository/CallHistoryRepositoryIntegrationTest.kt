package com.example.messenger.data.repository

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.example.messenger.data.local.obx.ObxConversation
import com.example.messenger.data.local.obx.ObxMessage
import com.example.messenger.data.local.obx.mappers.toObx
import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.call.CallType
import com.example.messenger.testutil.ObjectBoxExtension
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class CallHistoryRepositoryIntegrationTest {

    @JvmField
    @RegisterExtension
    val obx = ObjectBoxExtension()

    private val repository by lazy { CallHistoryRepository(obx.box(), obx.box()) }

    @BeforeEach
    fun seedConversation() {
        obx.box<ObxConversation>().put(
            Conversation(
                id = "c1",
                participantIds = listOf("me", "bob"),
                participantNames = listOf("Me", "Bob"),
                participantAvatars = listOf(null, "bob.png"),
            ).toObx(),
        )
    }

    private fun call(
        id: String,
        type: String,
        timestamp: Long,
        senderId: String = "me",
        conversationId: String = "c1",
        deleted: Boolean = false,
    ) {
        obx.box<ObxMessage>().put(
            ObxMessage(
                uid = id,
                conversationId = conversationId,
                senderId = senderId,
                type = type,
                timestamp = timestamp,
                deleted = deleted,
                callVideo = true,
                callDurationSeconds = 65,
            ),
        )
    }

    @Test
    fun `call history lists call messages newest first and skips deleted ones`() = runTest {
        call("m1", Message.TYPE_ENDED_CALL, timestamp = 1)
        call("m2", Message.TYPE_MISSED_CALL, timestamp = 3)
        call("m3", Message.TYPE_TEXT, timestamp = 2)
        call("m4", Message.TYPE_UNREACHED_CALL, timestamp = 4, deleted = true)

        val history = repository.getCallHistory("me").first()

        assertThat(history.map { it.id }).containsExactly("m2", "m1")
    }

    @Test
    fun `entry takes the partner's name and avatar from the conversation`() = runTest {
        call("m1", Message.TYPE_MISSED_CALL, timestamp = 1, senderId = "bob")

        val entry = repository.getCallHistory("me").first().single()

        assertThat(entry.partnerId).isEqualTo("bob")
        assertThat(entry.partnerName).isEqualTo("Bob")
        assertThat(entry.partnerAvatarUrl).isEqualTo("bob.png")
        assertThat(entry.callType).isEqualTo(CallType.MISSED)
        assertThat(entry.video).isEqualTo(true)
        assertThat(entry.durationSeconds).isEqualTo(65)
    }

    @Test
    fun `outgoing is true only for calls I placed`() = runTest {
        call("mine", Message.TYPE_ENDED_CALL, timestamp = 2, senderId = "me")
        call("theirs", Message.TYPE_ENDED_CALL, timestamp = 1, senderId = "bob")

        val history = repository.getCallHistory("me").first()

        assertThat(history.map { it.id to it.outgoing })
            .containsExactly("mine" to true, "theirs" to false)
    }

    @Test
    fun `a call whose conversation is gone is dropped`() = runTest {
        call("orphan", Message.TYPE_ENDED_CALL, timestamp = 2, conversationId = "gone")
        call("kept", Message.TYPE_ENDED_CALL, timestamp = 1)

        val history = repository.getCallHistory("me").first()

        assertThat(history.map { it.id }).containsExactly("kept")
    }
}
