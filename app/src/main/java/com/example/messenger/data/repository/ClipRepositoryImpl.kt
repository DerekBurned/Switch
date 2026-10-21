package com.example.messenger.data.repository

import android.util.Log
import com.example.messenger.data.crypto.E2eeMessageCodec
import com.example.messenger.data.local.obx.ObxConversation
import com.example.messenger.data.local.obx.ObxConversation_
import com.example.messenger.data.local.obx.ObxMessage
import com.example.messenger.data.local.obx.ObxMessage_
import com.example.messenger.data.local.obx.mappers.toDomain
import com.example.messenger.data.local.obx.mappers.toObx
import com.example.messenger.data.local.obx.queryFlow
import com.example.messenger.data.media.MediaCache
import com.example.messenger.data.media.MediaFileTransfer
import com.example.messenger.data.media.MediaTransferManager
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.data.remote.firebase.FirestoreService
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.voicemessage.Clip
import com.example.messenger.domain.repository.IClipRepository
import io.objectbox.Box
import io.objectbox.query.QueryBuilder.StringOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Singleton
class ClipRepositoryImpl @Inject constructor(
    private val messageBox: Box<ObxMessage>,
    private val conversationBox: Box<ObxConversation>,
    private val firestoreService: FirestoreService,
    private val manager: MediaTransferManager,
    private val fileTransfer: MediaFileTransfer,
    private val mediaCache: MediaCache,
    private val authService: FirebaseAuthService,
    private val codec: E2eeMessageCodec,
) : IClipRepository {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error ->
            Log.e(TAG, "clip task failed", error)
        },
    )

    private val downloads = InFlight<String, File?>(scope)

    private val sendJobs = ConcurrentHashMap<String, Job>()

    private val startedAt = System.currentTimeMillis()

    private val purged = ConcurrentHashMap.newKeySet<String>()

    init {
        scope.launch { recoverAfterRestart() }
        scope.launch {
            observeDeletedClipIds().collect { ids ->
                ids.filterNot { it in purged }.forEach(::purgeLocal)
            }
        }
    }

    override fun sendClip(conversationId: String, source: File, clip: Clip, replyTo: Message?) {
        if (clip.exceedsDurationLimit) {
            Log.w(TAG, "refusing clip of ${clip.durationMs}ms, limit ${clip.maxDurationMs}ms")
            source.delete()
            return
        }
        scope.launch {
            val messageId = UUID.randomUUID().toString()
            val dest = mediaCache.outboxFileFor(messageId, clip.fileExtension)
            if (!mediaCache.move(source, dest)) {
                Log.w(TAG, "could not stage clip file for $messageId")
                return@launch
            }
            val message = Message.Clip(
                id = messageId,
                conversationId = conversationId,
                senderId = authService.getCurrentUserId().orEmpty(),
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENDING,
                clip = clip,
                replyToMessageId = replyTo?.id,
                replyToText = replyTo?.replySnippet(),
                replyToSenderId = replyTo?.senderId,
            )
            messageBox.put(message.toObx())
            updateConversationPreview(message)
            launchSend(messageId)
        }
    }

    override suspend fun ensureLocal(messageId: String): File? = withContext(Dispatchers.IO) {
        val message = loadClip(messageId) ?: return@withContext null
        val outbox = mediaCache.outboxFileFor(messageId, message.clip.fileExtension)
        if (outbox.exists() && outbox.length() > 0) return@withContext outbox
        val file = mediaCache.clipFileFor(messageId, message.clip.fileExtension)
        if (file.exists() && file.length() > 0) return@withContext file
        val url = message.clip.url.takeIf { it.isNotBlank() } ?: return@withContext null
        try {
            downloads.join(messageId) {
                fileTransfer.download(messageId, url, message.clip.fileKey, file)
                file.takeIf { it.exists() && it.length() > 0 }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "clip download failed for $messageId", e)
            null
        }
    }

    override suspend fun cachedFile(messageId: String): File? = withContext(Dispatchers.IO) {
        val message = loadClip(messageId) ?: return@withContext null
        listOf(
            mediaCache.outboxFileFor(messageId, message.clip.fileExtension),
            mediaCache.clipFileFor(messageId, message.clip.fileExtension),
        ).firstOrNull { it.exists() && it.length() > 0 }
    }

    override suspend fun markPlayed(messageId: String) = withContext(Dispatchers.IO) {
        val row = findRow(messageId) ?: return@withContext
        if (row.clipPlayed) return@withContext
        row.clipPlayed = true
        messageBox.put(row)
    }

    override suspend fun nextVoiceClip(afterMessageId: String): Message.Clip? = withContext(Dispatchers.IO) {
        val current = loadClip(afterMessageId) ?: return@withContext null
        val candidates = messageBox.query(
            ObxMessage_.conversationId.equal(current.conversationId)
                .and(ObxMessage_.type.equal(Message.TYPE_VOICE_CLIP))
                .and(ObxMessage_.deleted.equal(false))
                .and(ObxMessage_.timestamp.greaterOrEqual(current.timestamp)),
        ).build().use { it.find() }.map { it.toDomain() }
        nextVoiceClipAfter(current, candidates)
    }

    override fun evictCachedCopy(messageId: String) {
        scope.launch {
            val message = loadClip(messageId) ?: return@launch
            mediaCache.clipFileFor(messageId, message.clip.fileExtension).delete()
        }
    }

    override fun observeDeletedClipIds(): Flow<Set<String>> =
        queryFlow {
            messageBox.query(
                ObxMessage_.type.oneOf(CLIP_TYPES, StringOrder.CASE_SENSITIVE)
                    .and(ObxMessage_.deleted.equal(true)),
            ).build()
        }
            .map { rows -> rows.mapTo(HashSet()) { it.uid } }
            .distinctUntilChanged()

    override fun deleteRemoteFile(message: Message.Clip) {
        scope.launch {
            val url = loadClip(message.id)?.clip?.url?.takeIf { it.isNotBlank() }
                ?: message.clip.url.takeIf { it.isNotBlank() }
                ?: return@launch
            manager.deleteRemote(url)
        }
    }

    private fun purgeLocal(messageId: String) {
        purged += messageId
        sendJobs.remove(messageId)?.cancel()
        manager.cancel(messageId)
        manager.clear(messageId)
        listOf(Clip.Voice.FILE_EXTENSION, Clip.Video.FILE_EXTENSION).forEach { extension ->
            mediaCache.clipFileFor(messageId, extension).delete()
            mediaCache.outboxFileFor(messageId, extension).delete()
        }
    }

    override fun retry(messageId: String) {
        if (sendJobs.containsKey(messageId)) return
        scope.launch {
            val claimed = messageBox.store.callInTx {
                val row = findRow(messageId)
                val current = row?.toDomain() as? Message.Clip
                if (row == null || current == null || current.deleted || current.status != MessageStatus.FAILED) {
                    false
                } else {
                    row.status = MessageStatus.SENDING.name
                    messageBox.put(row)
                    true
                }
            }
            if (claimed) launchSend(messageId)
        }
    }

    override fun cancelSend(messageId: String) {
        sendJobs.remove(messageId)?.cancel()
        manager.cancel(messageId)
        manager.clear(messageId)
        scope.launch {
            val message = loadClip(messageId) ?: return@launch
            if (message.status == MessageStatus.SENT || message.status == MessageStatus.READ) return@launch
            messageBox.query(ObxMessage_.uid.equal(messageId)).build().use { it.remove() }
            mediaCache.outboxFileFor(messageId, message.clip.fileExtension).delete()
        }
    }

    private fun launchSend(messageId: String) {
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                uploadAndSend(messageId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "clip send crashed for $messageId", e)
                markFailed(messageId)
            }
        }
        if (sendJobs.putIfAbsent(messageId, job) != null) {
            job.cancel()
            return
        }
        job.invokeOnCompletion { sendJobs.remove(messageId, job) }
        job.start()
    }

    private suspend fun uploadAndSend(messageId: String) {
        val message = loadClip(messageId) ?: return
        if (message.deleted) return
        adoptLegacyFile(message)
        val file = mediaCache.outboxFileFor(messageId, message.clip.fileExtension)
        if (!file.exists() || file.length() == 0L) {
            Log.w(TAG, "clip file missing for $messageId")
            markFailed(messageId)
            return
        }
        val encryptActive = codec.isEncryptable(message.conversationId)
        val path = storagePath(message.conversationId, messageId, message.clip)
        val uploaded = withTimeoutOrNull(clipUploadTimeout(file.length())) {
            fileTransfer.upload(messageId, file, path, encryptActive).getOrNull()
        }
        if (uploaded == null) {
            Log.w(TAG, "clip upload failed for $messageId")
            manager.cancel(messageId)
            manager.clear(messageId)
            markFailed(messageId)
            return
        }
        manager.clear(messageId)
        val stored = updateClip(messageId) { it.copy(clip = it.clip.withUpload(uploaded.url, uploaded.fileKey)) }
        if (stored == null) {
            manager.deleteRemote(uploaded.url)
            return
        }
        val result = firestoreService.sendMessage(
            stored.copy(status = MessageStatus.SENT),
            confirmWithin = CLIP_WRITE_TIMEOUT,
        )
        if (result.isFailure) {
            Log.e(TAG, "clip send failed for $messageId", result.exceptionOrNull())
            markFailed(messageId)
            return
        }
        updateClip(messageId) { current ->
            if (current.status == MessageStatus.SENDING || current.status == MessageStatus.FAILED) {
                current.copy(status = MessageStatus.SENT)
            } else {
                current
            }
        }
        mediaCache.move(file, mediaCache.clipFileFor(messageId, message.clip.fileExtension))
    }

    private fun recoverAfterRestart() {
        mediaCache.deleteStaleTemporaries(olderThan = startedAt)
        val unsent = messageBox.query(
            ObxMessage_.type.oneOf(CLIP_TYPES, StringOrder.CASE_SENSITIVE)
                .and(ObxMessage_.status.oneOf(UNSENT_STATUSES, StringOrder.CASE_SENSITIVE)),
        ).build().use { it.find() }.mapNotNull { it.toDomain() as? Message.Clip }
        unsent.forEach(::adoptLegacyFile)
        strandedClipIds(unsent, sendJobs.keys.toSet(), startedAt).forEach(::markFailed)
        mediaCache.outboxFiles().forEach { file ->
            val messageId = file.nameWithoutExtension
            if (sendJobs.containsKey(messageId)) return@forEach
            when (outboxFate(loadClip(messageId))) {
                OutboxFate.KEEP -> Unit
                OutboxFate.PROMOTE -> mediaCache.move(file, mediaCache.clipFileFor(messageId, file.extension))
                OutboxFate.DROP -> file.delete()
            }
        }
    }

    private fun adoptLegacyFile(message: Message.Clip) {
        val outbox = mediaCache.outboxFileFor(message.id, message.clip.fileExtension)
        if (outbox.exists() && outbox.length() > 0) return
        val legacy = mediaCache.clipFileFor(message.id, message.clip.fileExtension)
        if (legacy.exists() && legacy.length() > 0) mediaCache.move(legacy, outbox)
    }

    private fun updateClip(messageId: String, transform: (Message.Clip) -> Message.Clip): Message.Clip? =
        messageBox.store.callInTx {
            val row = findRow(messageId)
            val current = row?.toDomain() as? Message.Clip
            if (row == null || current == null || current.deleted) return@callInTx null
            transform(current).also { updated ->
                messageBox.put(updated.toObx().also { it.boxId = row.boxId })
            }
        }

    private fun storagePath(conversationId: String, messageId: String, clip: Clip): String =
        "chatMedia/$conversationId/$messageId/clip.${clip.fileExtension}"

    private fun findRow(messageId: String): ObxMessage? =
        messageBox.query(ObxMessage_.uid.equal(messageId)).build().use { it.findFirst() }

    private fun loadClip(messageId: String): Message.Clip? = findRow(messageId)?.toDomain() as? Message.Clip

    private fun setStatus(messageId: String, status: MessageStatus) {
        val row = findRow(messageId) ?: return
        row.status = status.name
        messageBox.put(row)
    }

    private fun markFailed(messageId: String) {
        setStatus(messageId, MessageStatus.FAILED)
    }

    private fun updateConversationPreview(message: Message.Clip) {
        val conv = conversationBox.query(ObxConversation_.uid.equal(message.conversationId)).build()
            .use { it.findFirst() } ?: return
        val label = message.clip.previewLabel
        conv.lastMessage = label
        conv.lastMessageSenderId = message.senderId
        conv.lastMessageTimestamp = message.timestamp
        conv.latestMessageText = label
        conv.latestMessageSenderId = message.senderId
        conv.latestMessageTimestamp = message.timestamp
        conversationBox.put(conv)
    }

    private companion object {
        const val TAG = "ClipRepositoryImpl"
        val CLIP_WRITE_TIMEOUT = 20.seconds
        val CLIP_TYPES = arrayOf(Message.TYPE_VOICE_CLIP, Message.TYPE_VIDEO_CLIP)
        val UNSENT_STATUSES = arrayOf(MessageStatus.SENDING.name, MessageStatus.FAILED.name)
    }
}

internal fun strandedClipIds(unsent: List<Message.Clip>, active: Set<String>, startedAt: Long): List<String> =
    unsent.filter { it.status == MessageStatus.SENDING && it.id !in active && it.timestamp < startedAt }
        .map { it.id }

internal fun nextVoiceClipAfter(current: Message.Clip, candidates: List<Message>): Message.Clip? =
    candidates.asSequence()
        .filterIsInstance<Message.Clip>()
        .filter { it.clip is Clip.Voice && !it.deleted && it.conversationId == current.conversationId && it.id != current.id }
        .filter { it.timestamp > current.timestamp || (it.timestamp == current.timestamp && it.id > current.id) }
        .minWithOrNull(compareBy<Message.Clip>({ it.timestamp }, { it.id }))

internal enum class OutboxFate { KEEP, PROMOTE, DROP }

internal fun outboxFate(row: Message.Clip?): OutboxFate = when {
    row == null || row.deleted -> OutboxFate.DROP
    row.status == MessageStatus.SENDING || row.status == MessageStatus.FAILED -> OutboxFate.KEEP
    else -> OutboxFate.PROMOTE
}

private val UPLOAD_BASE_TIMEOUT = 30.seconds

private const val UPLOAD_FLOOR_BYTES_PER_SECOND = 8_000L

internal fun clipUploadTimeout(sizeBytes: Long): Duration =
    UPLOAD_BASE_TIMEOUT + (sizeBytes.coerceAtLeast(0L) * 1000L / UPLOAD_FLOOR_BYTES_PER_SECOND).milliseconds

internal class InFlight<K : Any, V>(private val scope: CoroutineScope) {
    private val running = HashMap<K, Deferred<V>>()

    suspend fun join(key: K, block: suspend () -> V): V {
        val flight = synchronized(running) {
            running.getOrPut(key) {
                scope.async(start = CoroutineStart.LAZY) { block() }.also { started ->
                    started.invokeOnCompletion {
                        synchronized(running) { if (running[key] === started) running.remove(key) }
                    }
                }
            }
        }
        flight.start()
        return flight.await()
    }
}
