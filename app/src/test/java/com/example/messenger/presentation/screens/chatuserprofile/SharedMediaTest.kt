package com.example.messenger.presentation.screens.chatuserprofile

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.call.CallType
import com.example.messenger.domain.model.voicemessage.Clip
import org.junit.jupiter.api.Test

class SharedMediaTest {

    private fun photo(id: String) = MediaItem(id = id, kind = MediaItem.IMAGE)

    private fun video(id: String) = MediaItem(id = id, kind = MediaItem.VIDEO)

    private fun album(id: String, timestamp: Long, vararg items: MediaItem, deleted: Boolean = false) =
        Message.Media(id = id, conversationId = "c1", timestamp = timestamp, items = items.toList(), deleted = deleted)

    private fun voice(id: String, timestamp: Long, deleted: Boolean = false) =
        Message.Clip(id = id, conversationId = "c1", timestamp = timestamp, clip = Clip.Voice(), deleted = deleted)

    @Test
    fun `an album is split into photos and videos in album order`() {
        val media = sharedMediaOf(listOf(album("m1", 100, photo("p1"), video("v1"), photo("p2"))))

        assertThat(media.photos.map { it.item.id }).containsExactly("p1", "p2")
        assertThat(media.videos.map { it.item.id }).containsExactly("v1")
    }

    @Test
    fun `a media kind that is neither photo nor video lands in no grid`() {
        val media = sharedMediaOf(listOf(album("m1", 100, MediaItem(id = "doc", kind = "DOCUMENT"))))

        assertThat(media.photos).isEmpty()
        assertThat(media.videos).isEmpty()
    }

    @Test
    fun `newer messages come first`() {
        val media = sharedMediaOf(
            listOf(
                album("old", 100, photo("p-old")),
                album("new", 300, photo("p-new")),
                album("mid", 200, photo("p-mid")),
                voice("v-old", 100),
                voice("v-new", 300),
            ),
        )

        assertThat(media.photos.map { it.item.id }).containsExactly("p-new", "p-mid", "p-old")
        assertThat(media.voice.map { it.id }).containsExactly("v-new", "v-old")
    }

    @Test
    fun `only voice clips land in the voice tab`() {
        val media = sharedMediaOf(
            listOf(
                voice("voice", 100),
                Message.Clip(id = "round", timestamp = 200, clip = Clip.Video()),
                Message.Text(id = "text", timestamp = 300, text = "hi"),
                Message.Call(id = "call", timestamp = 400, callType = CallType.ENDED),
            ),
        )

        assertThat(media.voice.map { it.id }).containsExactly("voice")
        assertThat(media.photos).isEmpty()
        assertThat(media.videos).isEmpty()
    }

    @Test
    fun `deleted messages are left out`() {
        val media = sharedMediaOf(
            listOf(
                album("gone", 100, photo("p-gone"), deleted = true),
                album("kept", 200, photo("p-kept")),
                voice("v-gone", 300, deleted = true),
            ),
        )

        assertThat(media.photos.map { it.item.id }).containsExactly("p-kept")
        assertThat(media.voice).isEmpty()
    }
}
