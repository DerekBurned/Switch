package com.example.messenger.data.media

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.example.messenger.domain.model.voicemessage.ClipKind
import org.junit.jupiter.api.Test

class VoiceDraftMetaTest {

    @Test
    fun `metadata survives a round trip`() {
        val meta = VoiceDraftMeta(durationMs = 12_345L, waveform = listOf(1, 50, 99))
        assertThat(decodeVoiceDraftMeta(meta.encode())).isEqualTo(meta)
    }

    @Test
    fun `corrupt metadata is ignored`() {
        assertThat(decodeVoiceDraftMeta("{not json")).isNull()
        assertThat(decodeVoiceDraftMeta("")).isNull()
    }

    @Test
    fun `unknown fields are ignored`() {
        val raw = """{"durationMs":900,"waveform":[3],"addedLater":true}"""
        assertThat(decodeVoiceDraftMeta(raw)).isEqualTo(VoiceDraftMeta(durationMs = 900L, waveform = listOf(3)))
    }

    @Test
    fun `a draft saved before video support decodes as voice`() {
        val meta = decodeVoiceDraftMeta("""{"durationMs":1200,"waveform":[1,2]}""")
        assertThat(meta?.kind).isEqualTo(ClipKind.VOICE)
    }

    @Test
    fun `a video draft keeps its kind through a round trip`() {
        val meta = VoiceDraftMeta(durationMs = 3_000L, kind = ClipKind.VIDEO)
        assertThat(decodeVoiceDraftMeta(meta.encode())?.kind).isEqualTo(ClipKind.VIDEO)
    }

    @Test
    fun `a video draft stored without a waveform decodes with none`() {
        val meta = decodeVoiceDraftMeta("""{"durationMs":3000,"kind":"VIDEO"}""")
        assertThat(meta).isEqualTo(VoiceDraftMeta(durationMs = 3_000L, waveform = emptyList(), kind = ClipKind.VIDEO))
    }
}
