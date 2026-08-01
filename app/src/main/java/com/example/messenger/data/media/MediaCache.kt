package com.example.messenger.data.media

import android.content.Context
import android.net.Uri
import com.example.messenger.domain.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaCache @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val dir: File by lazy { File(context.cacheDir, DIR).apply { mkdirs() } }

    fun fileFor(itemId: String, kind: String): File = File(dir, fileName(itemId, kind))

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

    fun delete(itemId: String, kind: String) {
        fileFor(itemId, kind).delete()
    }

    companion object {
        const val DIR = "chat_media"

        fun fileName(itemId: String, kind: String): String {
            val ext = if (kind == MediaItem.VIDEO) "mp4" else "jpg"
            return "$itemId.$ext"
        }
    }
}
