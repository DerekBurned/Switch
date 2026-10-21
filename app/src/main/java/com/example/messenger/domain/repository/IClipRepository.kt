package com.example.messenger.domain.repository

import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.voicemessage.Clip
import kotlinx.coroutines.flow.Flow
import java.io.File

interface IClipRepository {

    fun sendClip(conversationId: String, source: File, clip: Clip, replyTo: Message? = null)

    suspend fun ensureLocal(messageId: String): File?

    suspend fun cachedFile(messageId: String): File?

    suspend fun markPlayed(messageId: String)

    suspend fun nextVoiceClip(afterMessageId: String): Message.Clip?

    fun evictCachedCopy(messageId: String)

    fun observeDeletedClipIds(): Flow<Set<String>>

    fun deleteRemoteFile(message: Message.Clip)

    fun retry(messageId: String)

    fun cancelSend(messageId: String)
}
