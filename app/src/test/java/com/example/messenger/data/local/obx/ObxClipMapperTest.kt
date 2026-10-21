package com.example.messenger.data.local.obx

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.example.messenger.data.local.obx.mappers.toDomain
import com.example.messenger.data.local.obx.mappers.toObx
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.voicemessage.Clip
import org.junit.jupiter.api.Test

class ObxClipMapperTest {

    private fun voiceMessage() = Message.Clip(
        id = "m1",
        conversationId = "c1",
        senderId = "me",
        timestamp = 5L,
        status = MessageStatus.SENT,
        clip = Clip.Voice(
            url = "https://example/clip.m4a",
            durationMs = 1200L,
            sizeBytes = 42L,
            fileKey = "key",
            waveform = listOf(1, 2, 3),
        ),
        played = true,
    )

    private fun videoMessage() = Message.Clip(
        id = "m2",
        conversationId = "c1",
        senderId = "me",
        clip = Clip.Video(url = "https://example/clip.mp4", durationMs = 900L),
    )

    @Test
    fun `voice clip is stored under the voice type`() {
        assertThat(voiceMessage().toObx().type).isEqualTo(Message.TYPE_VOICE_CLIP)
    }

    @Test
    fun `video clip is stored under the video type`() {
        assertThat(videoMessage().toObx().type).isEqualTo(Message.TYPE_VIDEO_CLIP)
    }

    @Test
    fun `voice clip survives a local round trip`() {
        val restored = voiceMessage().toObx().toDomain()
        assertThat(restored is Message.Clip).isTrue()
        val clip = (restored as Message.Clip).clip
        assertThat(clip is Clip.Voice).isTrue()
        assertThat((clip as Clip.Voice).waveform).isEqualTo(listOf(1, 2, 3))
        assertThat(clip.url).isEqualTo("https://example/clip.m4a")
        assertThat(clip.durationMs).isEqualTo(1200L)
        assertThat(clip.fileKey).isEqualTo("key")
    }

    @Test
    fun `played flag survives a local round trip`() {
        val restored = voiceMessage().toObx().toDomain() as Message.Clip
        assertThat(restored.played).isTrue()
    }

    @Test
    fun `video clip survives a local round trip`() {
        val restored = videoMessage().toObx().toDomain()
        assertThat(restored is Message.Clip).isTrue()
        val clip = (restored as Message.Clip).clip
        assertThat(clip is Clip.Video).isTrue()
        assertThat(clip.durationMs).isEqualTo(900L)
    }

    @Test
    fun `clip row with no payload degrades to text rather than throwing`() {
        val row = ObxMessage(uid = "m3", type = Message.TYPE_VOICE_CLIP, clipJson = null)
        assertThat(row.toDomain() is Message.Text).isTrue()
    }

    @Test
    fun `unknown stored type degrades to text`() {
        val row = ObxMessage(uid = "m4", type = "SOME_FUTURE_TYPE", text = "body")
        val restored = row.toDomain()
        assertThat(restored is Message.Text).isTrue()
        assertThat((restored as Message.Text).text).isEqualTo("body")
    }

    @Test
    fun `video clip carries no waveform`() {
        assertThat(videoMessage().toObx().clipJson?.waveform).isEqualTo(emptyList<Int>())
    }

    @Test
    fun `text message stores no clip payload`() {
        val text = Message.Text(id = "m5", conversationId = "c1", senderId = "me", text = "hi")
        assertThat(text.toObx().clipJson).isNull()
    }

    @Test
    fun `a voice reply keeps its quote locally`() {
        val reply = voiceMessage().copy(
            replyToMessageId = "m0",
            replyToText = "Are you coming?",
            replyToSenderId = "peer",
        )
        val restored = reply.toObx().toDomain() as Message.Clip
        assertThat(restored.replyToMessageId).isEqualTo("m0")
        assertThat(restored.replyToText).isEqualTo("Are you coming?")
        assertThat(restored.replyToSenderId).isEqualTo("peer")
    }
}
