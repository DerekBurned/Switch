package com.example.messenger.data.media

import android.content.Context
import android.net.Uri
import com.example.messenger.domain.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

internal fun isStaleTemporaryName(name: String): Boolean =
    name.startsWith("recording_") ||
        name.endsWith(".dl") ||
        name.endsWith(".part") ||
        name.endsWith(".enc")

@Singleton
class MediaCache @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val dir: File by lazy { File(context.cacheDir, DIR).apply { mkdirs() } }

    private val outboxDir: File by lazy { File(context.filesDir, OUTBOX_DIR).apply { mkdirs() } }

    fun fileFor(itemId: String, kind: String): File = File(dir, fileName(itemId, kind))

    fun clipFileFor(messageId: String, extension: String): File = File(dir, "$messageId.$extension")

    fun outboxFileFor(messageId: String, extension: String): File = File(outboxDir, "$messageId.$extension")

    fun outboxFiles(): List<File> = outboxDir.listFiles()?.filter { it.isFile }.orEmpty()

    fun newRecordingFile(extension: String): File =
        File(dir, "recording_${System.currentTimeMillis()}.$extension")

    fun exists(item: MediaItem): Boolean {
        val file = fileFor(item.id, item.kind)
        return file.exists() && file.length() > 0
    }

    fun copyFrom(uri: Uri, dest: File) {
        dest.parentFile?.mkdirs()
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open input stream for $uri")
        input.use { source ->
            dest.outputStream().use { output -> source.copyTo(output) }
        }
    }

    fun move(source: File, dest: File): Boolean {
        if (source == dest) return dest.exists() && dest.length() > 0
        if (!source.exists() || source.length() == 0L) return false
        dest.parentFile?.mkdirs()
        if (source.renameTo(dest)) return true
        return runCatching {
            source.copyTo(dest, overwrite = true)
            source.delete()
            true
        }.getOrDefault(false)
    }

    fun deleteStaleTemporaries(olderThan: Long) {
        listOf(dir, context.cacheDir).forEach { folder ->
            folder.listFiles()
                ?.filter { it.isFile && isStaleTemporaryName(it.name) && it.lastModified() < olderThan }
                ?.forEach { it.delete() }
        }
    }

    fun delete(itemId: String, kind: String) {
        fileFor(itemId, kind).delete()
    }

    companion object {
        const val DIR = "chat_media"
        const val OUTBOX_DIR = "clip_outbox"
        const val DRAFTS_DIR = "voice_drafts"

        fun fileName(itemId: String, kind: String): String {
            val ext = if (kind == MediaItem.VIDEO) "mp4" else "jpg"
            return "$itemId.$ext"
        }
    }
}
