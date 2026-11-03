package com.example.messenger.presentation.screens.chatuserprofile

import androidx.compose.runtime.Immutable
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.voicemessage.Clip

@Immutable
data class SharedMediaEntry(
    val message: Message.Media,
    val item: MediaItem,
)

@Immutable
data class SharedMedia(
    val photos: List<SharedMediaEntry> = emptyList(),
    val videos: List<SharedMediaEntry> = emptyList(),
    val voice: List<Message.Clip> = emptyList(),
)

fun sharedMediaOf(messages: Collection<Message>): SharedMedia {
    val newestFirst = messages.filterNot { it.deleted }.sortedByDescending { it.timestamp }
    val entries = newestFirst.filterIsInstance<Message.Media>()
        .flatMap { message -> message.items.map { SharedMediaEntry(message, it) } }
    return SharedMedia(
        photos = entries.filter { it.item.kind == MediaItem.IMAGE },
        videos = entries.filter { it.item.kind == MediaItem.VIDEO },
        voice = newestFirst.filterIsInstance<Message.Clip>().filter { it.clip is Clip.Voice },
    )
}
