package com.example.messenger.testutil.fakes

import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.voicemessage.Clip
import com.example.messenger.domain.repository.IClipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

class FakeClipRepository : IClipRepository {

    val remoteDeleted = mutableListOf<String>()
    val deletedIds = MutableStateFlow<Set<String>>(emptySet())

    override fun sendClip(conversationId: String, source: File, clip: Clip, replyTo: Message?) = Unit

    override suspend fun ensureLocal(messageId: String): File? = null

    override suspend fun cachedFile(messageId: String): File? = null

    override suspend fun markPlayed(messageId: String) = Unit

    override suspend fun nextVoiceClip(afterMessageId: String): Message.Clip? = null

    override fun evictCachedCopy(messageId: String) = Unit

    override fun observeDeletedClipIds(): Flow<Set<String>> = deletedIds

    override fun deleteRemoteFile(message: Message.Clip) {
        remoteDeleted += message.id
    }

    override fun retry(messageId: String) = Unit

    override fun cancelSend(messageId: String) = Unit
}
