package com.example.messenger.data.media

import android.content.Context
import com.example.messenger.domain.model.voicemessage.ClipKind
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
internal data class VoiceDraftMeta(
    val durationMs: Long,
    val waveform: List<Int> = emptyList(),
    val kind: ClipKind = ClipKind.VOICE,
)

private val draftJson = Json { ignoreUnknownKeys = true }

internal fun VoiceDraftMeta.encode(): String = draftJson.encodeToString(VoiceDraftMeta.serializer(), this)

internal fun decodeVoiceDraftMeta(raw: String): VoiceDraftMeta? =
    runCatching { draftJson.decodeFromString(VoiceDraftMeta.serializer(), raw) }.getOrNull()

@Singleton
class VoiceDraftStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaCache: MediaCache,
) {

    private val drafts = MutableStateFlow<Map<String, ClipRecording>>(emptyMap())

    fun observe(conversationId: String): Flow<ClipRecording?> =
        drafts.map { it[conversationId] }.distinctUntilChanged()

    suspend fun load(conversationId: String) = withContext(Dispatchers.IO) {
        if (drafts.value.containsKey(conversationId)) return@withContext
        val metaFile = metaFileFor(conversationId)
        val meta = metaFile.takeIf { it.exists() }
            ?.let { runCatching { decodeVoiceDraftMeta(it.readText()) }.getOrNull() }
        val media = meta?.let { mediaFileFor(conversationId, it.kind) }
        if (meta == null || media == null || !media.exists() || media.length() == 0L) {
            deleteMedia(conversationId)
            metaFile.delete()
            return@withContext
        }
        val recording = ClipRecording(
            conversationId = conversationId,
            file = media,
            durationMs = meta.durationMs,
            waveform = meta.waveform,
            kind = meta.kind,
        )
        drafts.update { current -> if (current.containsKey(conversationId)) current else current + (conversationId to recording) }
    }

    fun save(recording: ClipRecording) {
        val media = mediaFileFor(recording.conversationId, recording.kind)
        ClipKind.entries
            .filter { it != recording.kind }
            .forEach { mediaFileFor(recording.conversationId, it).delete() }
        if (!mediaCache.move(recording.file, media)) {
            recording.file.delete()
            return
        }
        runCatching {
            metaFileFor(recording.conversationId)
                .writeText(VoiceDraftMeta(recording.durationMs, recording.waveform, recording.kind).encode())
        }
        drafts.update { it + (recording.conversationId to recording.copy(file = media)) }
    }

    fun take(conversationId: String): ClipRecording? {
        val draft = drafts.value[conversationId] ?: return null
        drafts.update { it - conversationId }
        metaFileFor(conversationId).delete()
        return draft
    }

    fun discard(conversationId: String) {
        drafts.update { it - conversationId }
        deleteMedia(conversationId)
        metaFileFor(conversationId).delete()
    }

    fun clear() {
        drafts.value = emptyMap()
        runCatching { draftsDir().deleteRecursively() }
    }

    private fun draftsDir(): File = File(context.filesDir, MediaCache.DRAFTS_DIR).apply { mkdirs() }

    private fun mediaFileFor(conversationId: String, kind: ClipKind): File =
        File(draftsDir(), "$conversationId.${kind.extension}")

    private fun deleteMedia(conversationId: String) {
        ClipKind.entries.forEach { mediaFileFor(conversationId, it).delete() }
    }

    private fun metaFileFor(conversationId: String): File = File(draftsDir(), "$conversationId.json")
}
