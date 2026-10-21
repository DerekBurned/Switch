package com.example.messenger.data.media

import android.content.Context
import android.util.Log
import com.example.messenger.data.crypto.MediaCipher
import com.example.messenger.data.crypto.b64
import com.example.messenger.data.crypto.unb64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaFileTransfer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val manager: MediaTransferManager,
) {

    data class UploadedFile(val url: String, val fileKey: String?)

    suspend fun upload(
        itemId: String,
        source: File,
        storagePath: String,
        encrypt: Boolean,
    ): Result<UploadedFile> {
        val fileKey = if (encrypt) MediaCipher.generateFileKey() else null
        val sealed = fileKey?.let { File(context.cacheDir, "$itemId.enc") }
        try {
            if (fileKey != null && sealed != null) {
                runCatching { MediaCipher.encrypt(fileKey, source, sealed) }.onFailure { error ->
                    Log.w(TAG, "media encrypt failed: $itemId", error)
                    return Result.failure(error)
                }
            }
            return manager.upload(itemId, sealed ?: source, storagePath)
                .map { url -> UploadedFile(url = url, fileKey = fileKey?.let(::b64)) }
        } finally {
            sealed?.delete()
        }
    }

    suspend fun download(itemId: String, url: String, fileKey: String?, dest: File): Result<Unit> {
        dest.parentFile?.mkdirs()
        val result = if (fileKey.isNullOrBlank()) {
            val partial = File.createTempFile(dest.name + ".", ".dl", dest.parentFile)
            manager.download(itemId, url, partial)
                .mapCatching { Files.move(partial.toPath(), dest.toPath(), ATOMIC_MOVE, REPLACE_EXISTING); Unit }
                .also { partial.delete() }
        } else {
            val encrypted = File.createTempFile("$itemId.", ".enc.dl", context.cacheDir)
            manager.download(itemId, url, encrypted)
                .mapCatching { MediaCipher.decrypt(unb64(fileKey), encrypted, dest) }
                .also { encrypted.delete() }
        }
        result.onFailure { Log.w(TAG, "media download failed: $itemId", it) }
        manager.clear(itemId)
        return result
    }

    private companion object {
        const val TAG = "MediaFileTransfer"
    }
}
