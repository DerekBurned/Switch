package com.example.messenger.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.messenger.data.crypto.E2eeMessageCodec
import com.example.messenger.data.crypto.MediaCipher
import com.example.messenger.data.crypto.b64
import com.example.messenger.data.crypto.unb64
import com.example.messenger.data.local.obx.ObxConversation
import com.example.messenger.data.local.obx.ObxConversation_
import com.example.messenger.data.local.obx.ObxMessage
import com.example.messenger.data.local.obx.ObxMessage_
import com.example.messenger.data.local.obx.ObxSyncQueueItem
import com.example.messenger.data.local.obx.toDomain
import com.example.messenger.data.local.obx.toObx
import com.example.messenger.data.media.MediaCache
import com.example.messenger.data.media.MediaMetadata
import com.example.messenger.data.media.MediaTransferManager
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.data.remote.firebase.FirestoreService
import com.example.messenger.domain.model.CallType
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.MediaTransfer
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.sync.SyncAction
import com.example.messenger.domain.model.sync.SyncEntityType
import com.example.messenger.domain.repository.IMediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.objectbox.Box
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val messageBox: Box<ObxMessage>,
    private val conversationBox: Box<ObxConversation>,
    private val syncQueueBox: Box<ObxSyncQueueItem>,
    private val firestoreService: FirestoreService,
    private val manager: MediaTransferManager,
    private val mediaCache: MediaCache,
    private val mediaMetadata: MediaMetadata,
    private val authService: FirebaseAuthService,
    private val codec: E2eeMessageCodec,
) : IMediaRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cancelledItems = ConcurrentHashMap.newKeySet<String>()

    override val transfers: StateFlow<Map<String, MediaTransfer>> = manager.transfers

    override fun sendMediaMessage(conversationId: String, uris: List<String>, caption: String) {
        scope.launch {
            val message = buildLocalMessage(conversationId, uris.take(MAX_GROUP), caption)
            if (message.items.isEmpty()) return@launch
            messageBox.put(message.toObx())
            updateConversationPreview(message)
            uploadAndSend(message.id)
        }
    }

    override fun downloadMedia(item: MediaItem) {
        scope.launch {
            val dest = mediaCache.fileFor(item.id, item.kind)
            val fileKey = item.fileKey
            if (fileKey.isNullOrBlank()) {
                manager.download(item.id, item.url, dest)
            } else {
                val encrypted = File(context.cacheDir, "${item.id}.enc.dl")
                manager.download(item.id, item.url, encrypted)
                if (encrypted.exists() && encrypted.length() > 0) {
                    runCatching { MediaCipher.decrypt(unb64(fileKey), encrypted, dest) }
                        .onFailure { Log.w(TAG, "media decrypt failed: ${item.id}", it) }
                }
                encrypted.delete()
            }
            manager.clear(item.id)
        }
    }

    override fun cancelUpload(messageId: String, itemId: String) {
        cancelledItems.add(itemId)
        manager.cancel(itemId)
        manager.clear(itemId)
        scope.launch {
            val message = loadMessage(messageId) ?: return@launch
            if (message !is Message.Media) return@launch
            val item = message.items.firstOrNull { it.id == itemId }
            if (item != null) mediaCache.delete(itemId, item.kind)
            val remaining = message.items.filterNot { it.id == itemId }
            if (remaining.isEmpty()) {
                deleteLocal(messageId)
            } else {
                putMedia(messageId, remaining)
            }
        }
    }

    override fun cancelDownload(itemId: String) {
        manager.cancel(itemId)
        manager.clear(itemId)
    }

    override fun retry(messageId: String) {
        scope.launch {
            val msg = loadMessage(messageId) as? Message.Media ?: return@launch
            Log.d(TAG, "retry: msg=$messageId items=${msg.items.size}")
            msg.items.forEach { cancelledItems.remove(it.id) }
            setStatus(messageId, MessageStatus.SENDING)
            uploadAndSend(messageId)
        }
    }

    private fun buildLocalMessage(
        conversationId: String,
        uris: List<String>,
        caption: String,
    ): Message.Media {
        val senderId = authService.getCurrentUserId().orEmpty()
        val items = uris.mapNotNull { uriString ->
            runCatching {
                val uri = Uri.parse(uriString)
                val id = UUID.randomUUID().toString()
                val kind = resolveKind(uri)
                val dest = mediaCache.fileFor(id, kind)
                mediaCache.copyFrom(uri, dest)
                val info = mediaMetadata.extract(Uri.fromFile(dest), kind)
                MediaItem(
                    id = id,
                    kind = kind,
                    blurHash = info.blurHash,
                    width = info.width,
                    height = info.height,
                    durationMs = info.durationMs,
                    sizeBytes = dest.length(),
                )
            }.getOrNull()
        }
        return Message.Media(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = senderId,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENDING,
            items = items,
            caption = caption.trim(),
        )
    }

    private suspend fun uploadAndSend(messageId: String) {
        val original = loadMessage(messageId) as? Message.Media ?: return
        Log.d(TAG, "uploadAndSend start: msg=$messageId items=${original.items.size}")

        val encryptActive = codec.isEncryptable(original.conversationId)

        coroutineScope {
            val jobs = original.items
                .filterNot { cancelledItems.contains(it.id) }
                .map { item ->
                    launch {
                        val file = mediaCache.fileFor(item.id, item.kind)
                        val path = storagePath(original.conversationId, messageId, item)
                        val fileKey = if (encryptActive) MediaCipher.generateFileKey() else null
                        val uploadFile = if (fileKey != null) {
                            File(context.cacheDir, "${item.id}.enc").also {
                                MediaCipher.encrypt(fileKey, file, it)
                            }
                        } else {
                            file
                        }
                        val result = manager.upload(item.id, uploadFile, path)
                        if (fileKey != null) uploadFile.delete()
                        if (result.isSuccess) {
                            val url = result.getOrThrow()
                            val current = loadMessage(messageId) as? Message.Media
                            putMedia(messageId, current?.items.orEmpty().map {
                                if (it.id == item.id) {
                                    it.copy(url = url, storagePath = path, fileKey = fileKey?.let(::b64))
                                } else {
                                    it
                                }
                            })
                            manager.clear(item.id)
                        }
                    }
                }
            val completed = withTimeoutOrNull(SEND_TIMEOUT_MS) { jobs.joinAll(); true } ?: false
            if (!completed) {
                Log.w(TAG, "uploadAndSend timeout after ${SEND_TIMEOUT_MS}ms: msg=$messageId")
                jobs.forEach { it.cancel() }
            }
        }

        val finalMessage = loadMessage(messageId) as? Message.Media ?: return
        if (finalMessage.items.isEmpty()) {
            deleteLocal(messageId)
            return
        }

        val succeeded = finalMessage.items.filter { it.url.isNotBlank() }
        val failed = finalMessage.items.filter { it.url.isBlank() }
        failed.forEach { manager.cancel(it.id) }
        Log.d(TAG, "uploadAndSend partition: msg=$messageId succeeded=${succeeded.size} failed=${failed.size}")

        when {
            succeeded.isEmpty() -> markFailed(messageId)
            failed.isEmpty() -> {
                val sent = finalMessage.copy(status = MessageStatus.SENT)
                sendOrFail(sent)
            }
            else -> {
                val sentPart = finalMessage.copy(items = succeeded, status = MessageStatus.SENT)
                messageBox.put(sentPart.toObx())
                sendOrFail(sentPart)
                splitFailed(finalMessage, failed)
            }
        }
    }

    private suspend fun sendOrFail(message: Message.Media) {
        val result = firestoreService.sendMessage(message)
        if (result.isSuccess) {
            Log.d(TAG, "firestore send OK: msg=${message.id}")
            messageBox.put(message.toObx())
        } else {
            Log.e(TAG, "firestore send FAILED: msg=${message.id}", result.exceptionOrNull())
            markFailed(message.id)
        }
    }

    private fun splitFailed(source: Message.Media, failed: List<MediaItem>) {
        val failedId = UUID.randomUUID().toString()
        val failedMessage = Message.Media(
            id = failedId,
            conversationId = source.conversationId,
            senderId = source.senderId,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.FAILED,
            items = failed,
            caption = "",
        )
        messageBox.put(failedMessage.toObx())
        Log.d(TAG, "split failed bubble: id=$failedId items=${failed.size}")
    }

    private fun setStatus(messageId: String, status: MessageStatus) {
        val existing = messageBox.query(ObxMessage_.uid.equal(messageId)).build()
            .use { it.findFirst() } ?: return
        existing.status = status.name
        messageBox.put(existing)
    }

    private fun resolveKind(uri: Uri): String {
        val type = context.contentResolver.getType(uri).orEmpty()
        return if (type.startsWith("video")) MediaItem.VIDEO else MediaItem.IMAGE
    }

    private fun storagePath(conversationId: String, messageId: String, item: MediaItem): String {
        val ext = if (item.kind == MediaItem.VIDEO) "mp4" else "jpg"
        return "chatMedia/$conversationId/$messageId/${item.id}.$ext"
    }

    private fun loadMessage(messageId: String): Message? =
        messageBox.query(ObxMessage_.uid.equal(messageId)).build()
            .use { it.findFirst() }?.toDomain()

    private fun putMedia(messageId: String, media: List<MediaItem>) {
        val existing = messageBox.query(ObxMessage_.uid.equal(messageId)).build()
            .use { it.findFirst() } ?: return
        existing.mediaItemsJson = media
        messageBox.put(existing)
    }

    private fun markFailed(messageId: String) {
        val existing = messageBox.query(ObxMessage_.uid.equal(messageId)).build()
            .use { it.findFirst() } ?: return
        existing.status = MessageStatus.FAILED.name
        messageBox.put(existing)
    }

    private fun deleteLocal(messageId: String) {
        messageBox.query(ObxMessage_.uid.equal(messageId)).build()
            .use { it.remove() }
    }

    private fun enqueueRetry(messageId: String) {
        syncQueueBox.put(
            ObxSyncQueueItem(
                entityType = SyncEntityType.MESSAGE,
                entityId = messageId,
                action = SyncAction.SEND,
                timestamp = System.currentTimeMillis(),
            ),
        )
    }

    private fun updateConversationPreview(message: Message.Media) {
        val conv = conversationBox.query(ObxConversation_.uid.equal(message.conversationId)).build()
            .use { it.findFirst() } ?: return
        val label = previewLabel(message)
        conv.lastMessage = label
        conv.lastMessageSenderId = message.senderId
        conv.lastMessageTimestamp = message.timestamp
        conv.latestMessageText = label
        conv.latestMessageSenderId = message.senderId
        conv.latestMessageTimestamp = message.timestamp
        conversationBox.put(conv)
    }

    private fun previewLabel(message: Message.Media): String {
        if (message.caption.isNotBlank()) return message.caption
        val count = message.items.size
        return when {
            count == 1 && message.items.first().kind == MediaItem.VIDEO -> "Video"
            count == 1 -> "Photo"
            message.items.all { it.kind == MediaItem.IMAGE } -> "$count photos"
            message.items.all { it.kind == MediaItem.VIDEO } -> "$count videos"
            else -> "$count media"
        }
    }

    private companion object {
        const val TAG = "MediaRepositoryImpl"
        const val MAX_GROUP = 15
        const val SEND_TIMEOUT_MS = 35_000L
    }
}
