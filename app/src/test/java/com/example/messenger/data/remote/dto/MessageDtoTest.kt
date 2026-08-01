package com.example.messenger.data.remote.dto

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus
import org.junit.jupiter.api.Test

class MessageDtoTest {

    @Test
    fun `pending local write is reported as SENDING regardless of stored status`() {
        val dto = RemoteMessageDto(
            id = "m5",
            conversationId = "c1",
            senderId = "me",
            status = "SENT",
            type = Message.TYPE_TEXT,
            text = "hi",
        )
        assertThat(dto.toDomain(hasPendingWrites = true).status).isEqualTo(MessageStatus.SENDING)
    }

    @Test
    fun `server-acked write keeps its stored status`() {
        val dto = RemoteMessageDto(
            id = "m6",
            conversationId = "c1",
            senderId = "me",
            status = "SENT",
            type = Message.TYPE_TEXT,
            text = "hi",
        )
        assertThat(dto.toDomain(hasPendingWrites = false).status).isEqualTo(MessageStatus.SENT)
    }

    @Test
    fun `server-acked read message is not downgraded to sending`() {
        val dto = RemoteMessageDto(
            id = "m7",
            conversationId = "c1",
            senderId = "me",
            status = "READ",
            type = Message.TYPE_TEXT,
            text = "hi",
        )
        assertThat(dto.toDomain(hasPendingWrites = false).status).isEqualTo(MessageStatus.READ)
    }

    @Test
    fun `read status maps to isRead true even when isRead field is false`() {
        val dto = RemoteMessageDto(
            id = "m1",
            conversationId = "c1",
            senderId = "other",
            status = "READ",
            isRead = false,
            type = Message.TYPE_TEXT,
            text = "hi",
        )
        assertThat(dto.toDomain().isRead).isTrue()
    }

    @Test
    fun `sent status keeps isRead false`() {
        val dto = RemoteMessageDto(
            id = "m2",
            conversationId = "c1",
            senderId = "other",
            status = "SENT",
            isRead = false,
            type = Message.TYPE_TEXT,
            text = "hi",
        )
        assertThat(dto.toDomain().isRead).isFalse()
    }

    @Test
    fun `explicit isRead true is preserved when status is sent`() {
        val dto = RemoteMessageDto(
            id = "m3",
            conversationId = "c1",
            senderId = "other",
            status = "SENT",
            isRead = true,
            type = Message.TYPE_TEXT,
            text = "hi",
        )
        assertThat(dto.toDomain().isRead).isTrue()
    }

    @Test
    fun `read status maps to isRead true for call messages`() {
        val dto = RemoteMessageDto(
            id = "m4",
            conversationId = "c1",
            senderId = "other",
            status = "READ",
            isRead = false,
            type = Message.TYPE_MISSED_CALL,
        )
        assertThat(dto.toDomain().isRead).isTrue()
    }
}
