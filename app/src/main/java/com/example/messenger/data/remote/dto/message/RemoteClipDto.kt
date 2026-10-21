package com.example.messenger.data.remote.dto.message

import com.example.messenger.domain.model.voicemessage.Clip

data class RemoteClipDto(
    val url: String = "",
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val waveform: List<Int> = emptyList(),
)

fun Clip.toRemoteDto(): RemoteClipDto = when (this) {
    is Clip.Voice -> RemoteClipDto(
        url = url,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        waveform = waveform,
    )
    is Clip.Video -> RemoteClipDto(
        url = url,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
    )
}

fun RemoteClipDto.toVoiceClip(): Clip = Clip.Voice(
    url = url,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    waveform = waveform,
)

fun RemoteClipDto.toVideoClip(): Clip = Clip.Video(
    url = url,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
)
