package com.example.messenger.domain.model.voicemessage

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class ClipPreviewLabelTest {

    @Test
    fun `the persisted voice label never changes`() {
        assertThat(Clip.Voice.PREVIEW_LABEL).isEqualTo("Voice message")
    }

    @Test
    fun `the persisted video label never changes`() {
        assertThat(Clip.Video.PREVIEW_LABEL).isEqualTo("Video message")
    }

    @Test
    fun `voice clips label and store themselves as voice`() {
        val clip = Clip.Voice()
        assertThat(clip.previewLabel).isEqualTo(Clip.Voice.PREVIEW_LABEL)
        assertThat(clip.fileExtension).isEqualTo("m4a")
    }

    @Test
    fun `video clips label and store themselves as video`() {
        val clip = Clip.Video()
        assertThat(clip.previewLabel).isEqualTo(Clip.Video.PREVIEW_LABEL)
        assertThat(clip.fileExtension).isEqualTo("mp4")
    }
}
