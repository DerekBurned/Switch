package com.example.messenger.data.remote.dto

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.example.messenger.data.remote.dto.message.RemoteClipDto
import com.example.messenger.data.remote.dto.message.RemoteMessageDto
import com.example.messenger.data.remote.dto.message.toDomain
import com.example.messenger.data.remote.dto.message.toRemoteDto
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.voicemessage.Clip
import org.junit.jupiter.api.Test

class ClipDtoTest {

    private fun voiceMessage(fileKey: String? = null) = Message.Clip(
        id = "m1",
        conversationId = "c1",
        senderId = "me",
        clip = Clip.Voice(
            url = "https://example/clip.m4a",
            durationMs = 1200L,
            sizeBytes = 42L,
            fileKey = fileKey,
            waveform = listOf(1, 2, 3),
        ),
    )

    @Test
    fun `voice clip goes on the wire under the voice type`() {
        val dto = voiceMessage().toRemoteDto()
        assertThat(dto.type).isEqualTo(Message.TYPE_VOICE_CLIP)
        assertThat(dto.clip).isNotNull()
        assertThat(dto.clip?.waveform).isEqualTo(listOf(1, 2, 3))
    }

    @Test
    fun `video clip goes on the wire under the video type`() {
        val message = Message.Clip(
            id = "m2",
            conversationId = "c1",
            senderId = "me",
            clip = Clip.Video(url = "https://example/clip.mp4", durationMs = 900L),
        )
        val dto = message.toRemoteDto()
        assertThat(dto.type).isEqualTo(Message.TYPE_VIDEO_CLIP)
        assertThat(dto.clip?.durationMs).isEqualTo(900L)
    }

    @Test
    fun `voice clip survives a wire round trip`() {
        val restored = voiceMessage().toRemoteDto().toDomain()
        assertThat(restored is Message.Clip).isTrue()
        val clip = (restored as Message.Clip).clip
        assertThat(clip is Clip.Voice).isTrue()
        assertThat((clip as Clip.Voice).waveform).isEqualTo(listOf(1, 2, 3))
        assertThat(clip.durationMs).isEqualTo(1200L)
    }

    @Test
    fun `file key never travels on the wire`() {
        val restored = voiceMessage(fileKey = "secret").toRemoteDto().toDomain() as Message.Clip
        assertThat(restored.clip.fileKey).isNull()
    }

    @Test
    fun `a video clip from a newer client is decoded rather than dropped`() {
        val dto = RemoteMessageDto(
            id = "m3",
            conversationId = "c1",
            senderId = "them",
            type = Message.TYPE_VIDEO_CLIP,
            clip = RemoteClipDto(url = "https://example/clip.mp4", durationMs = 700L),
        )
        val restored = dto.toDomain()
        assertThat(restored is Message.Clip).isTrue()
        assertThat((restored as Message.Clip).clip is Clip.Video).isTrue()
    }

    @Test
    fun `a clip type with no payload degrades to text`() {
        val dto = RemoteMessageDto(id = "m4", type = Message.TYPE_VOICE_CLIP, clip = null)
        assertThat(dto.toDomain() is Message.Text).isTrue()
    }

    @Test
    fun `an unknown message type degrades to text`() {
        val dto = RemoteMessageDto(id = "m5", type = "SOME_FUTURE_TYPE", text = "body")
        val restored = dto.toDomain()
        assertThat(restored is Message.Text).isTrue()
        assertThat((restored as Message.Text).text).isEqualTo("body")
    }

    @Test
    fun `pending clip write is reported as sending`() {
        val dto = voiceMessage().toRemoteDto()
        assertThat(dto.toDomain(hasPendingWrites = true).status)
            .isEqualTo(com.example.messenger.domain.model.MessageStatus.SENDING)
    }

    @Test
    fun `a voice reply keeps its quote on the wire`() {
        val reply = voiceMessage().copy(
            replyToMessageId = "m0",
            replyToText = "Are you coming?",
            replyToSenderId = "peer",
        )
        val restored = reply.toRemoteDto().toDomain() as Message.Clip
        assertThat(restored.replyToMessageId).isEqualTo("m0")
        assertThat(restored.replyToText).isEqualTo("Are you coming?")
        assertThat(restored.replyToSenderId).isEqualTo("peer")
    }
}
