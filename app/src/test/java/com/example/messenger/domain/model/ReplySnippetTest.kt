package com.example.messenger.domain.model

import assertk.assertThat
import assertk.assertions.hasLength
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.example.messenger.domain.model.voicemessage.Clip
import org.junit.jupiter.api.Test

class ReplySnippetTest {

    @Test
    fun `text replies are cut to 200 characters`() {
        val message = Message.Text(text = "a".repeat(500))
        assertThat(message.replySnippet()!!).hasLength(Message.REPLY_SNIPPET_MAX)
    }

    @Test
    fun `media replies use the caption`() {
        val message = Message.Media(caption = "Sunset at the lake")
        assertThat(message.replySnippet()).isEqualTo("Sunset at the lake")
    }

    @Test
    fun `captionless media replies read Media`() {
        assertThat(Message.Media().replySnippet()).isEqualTo("Media")
    }

    @Test
    fun `voice replies read Voice message`() {
        val message = Message.Clip(clip = Clip.Voice())
        assertThat(message.replySnippet()).isEqualTo("Voice message")
    }

    @Test
    fun `video replies read Video message`() {
        val message = Message.Clip(clip = Clip.Video())
        assertThat(message.replySnippet()).isEqualTo("Video message")
    }

    @Test
    fun `calls have no snippet`() {
        assertThat(Message.Call().replySnippet()).isNull()
    }
}
