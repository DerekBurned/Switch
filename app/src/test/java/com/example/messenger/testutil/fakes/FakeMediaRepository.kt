package com.example.messenger.testutil.fakes

import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.MediaTransfer
import com.example.messenger.domain.repository.IMediaRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeMediaRepository : IMediaRepository {

    override val transfers = MutableStateFlow<Map<String, MediaTransfer>>(emptyMap())
    val sentMedia = mutableListOf<Triple<String, List<String>, String>>()

    override fun sendMediaMessage(conversationId: String, uris: List<String>, caption: String) {
        sentMedia += Triple(conversationId, uris, caption)
    }

    override fun downloadMedia(item: MediaItem) = Unit

    override fun cancelUpload(messageId: String, itemId: String) = Unit

    override fun cancelDownload(itemId: String) = Unit

    override fun retry(messageId: String) = Unit
}
