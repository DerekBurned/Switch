package com.example.messenger.presentation.screens.chat

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.example.messenger.domain.model.MediaTransfer
import com.example.messenger.domain.model.MessageStatus
import org.junit.jupiter.api.Test

class VoiceButtonStateTest {

    private fun button(
        transfer: MediaTransfer? = null,
        status: MessageStatus = MessageStatus.SENT,
        isMe: Boolean = false,
        isLoading: Boolean = false,
        playFailed: Boolean = false,
        isPlaying: Boolean = false,
    ) = voiceButton(transfer, status, isMe, isLoading, playFailed, isPlaying)

    @Test
    fun `an upload in progress offers cancel`() {
        assertThat(button(MediaTransfer.Uploading(0.3f), MessageStatus.SENDING, isMe = true))
            .isEqualTo(VoiceButton.CANCEL_SEND)
    }

    @Test
    fun `a download in progress shows its progress`() {
        assertThat(button(MediaTransfer.Downloading(0.5f))).isEqualTo(VoiceButton.DOWNLOADING)
    }

    @Test
    fun `a failed outgoing clip offers a send retry`() {
        assertThat(button(status = MessageStatus.FAILED, isMe = true)).isEqualTo(VoiceButton.RETRY_SEND)
    }

    @Test
    fun `a loading clip shows a spinner`() {
        assertThat(button(isLoading = true)).isEqualTo(VoiceButton.LOADING)
    }

    @Test
    fun `a clip that failed to play offers a retry`() {
        assertThat(button(playFailed = true)).isEqualTo(VoiceButton.RETRY_PLAY)
    }

    @Test
    fun `a playing clip offers pause`() {
        assertThat(button(isPlaying = true)).isEqualTo(VoiceButton.PAUSE)
    }

    @Test
    fun `an idle clip offers play`() {
        assertThat(button()).isEqualTo(VoiceButton.PLAY)
    }

    @Test
    fun `uploading wins over everything`() {
        val busy = button(
            transfer = MediaTransfer.Uploading(0.1f),
            status = MessageStatus.FAILED,
            isMe = true,
            isLoading = true,
            playFailed = true,
            isPlaying = true,
        )
        assertThat(busy).isEqualTo(VoiceButton.CANCEL_SEND)
    }

    @Test
    fun `an incoming clip never offers a send retry`() {
        assertThat(button(status = MessageStatus.FAILED, isMe = false)).isEqualTo(VoiceButton.PLAY)
    }
}
