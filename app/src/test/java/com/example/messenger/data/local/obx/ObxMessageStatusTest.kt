package com.example.messenger.data.local.obx

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.example.messenger.domain.model.MessageStatus
import org.junit.jupiter.api.Test

class ObxMessageStatusTest {

    private fun message(status: String) = ObxMessage(
        uid = "m1",
        conversationId = "c1",
        senderId = "me",
        text = "hi",
        status = status,
    )

    @Test
    fun `sending status is preserved when read back from local store`() {
        assertThat(message("SENDING").toDomain().status).isEqualTo(MessageStatus.SENDING)
    }

    @Test
    fun `read status is preserved`() {
        assertThat(message("READ").toDomain().status).isEqualTo(MessageStatus.READ)
    }

    @Test
    fun `failed status is preserved`() {
        assertThat(message("FAILED").toDomain().status).isEqualTo(MessageStatus.FAILED)
    }

    @Test
    fun `sent status stays sent`() {
        assertThat(message("SENT").toDomain().status).isEqualTo(MessageStatus.SENT)
    }

    @Test
    fun `legacy delivered maps to sent`() {
        assertThat(message("DELIVERED").toDomain().status).isEqualTo(MessageStatus.SENT)
    }
}
