package com.example.messenger.domain.model.voicemessage

enum class ClipKind(val extension: String) {
    VOICE(Clip.Voice.FILE_EXTENSION),
    VIDEO(Clip.Video.FILE_EXTENSION);

    fun toggled(): ClipKind = if (this == VOICE) VIDEO else VOICE
}
