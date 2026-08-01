package com.example.messenger.domain.repository

import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.MediaTransfer
import kotlinx.coroutines.flow.StateFlow

interface IMediaRepository {
    val transfers: StateFlow<Map<String, MediaTransfer>>

    fun sendMediaMessage(conversationId: String, uris: List<String>, caption: String)

    fun downloadMedia(item: MediaItem)

    fun cancelUpload(messageId: String, itemId: String)

    fun cancelDownload(itemId: String)

    fun retry(messageId: String)
}
