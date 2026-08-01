package com.example.messenger.data.media

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BlurHashCache {

    private const val DECODE_SIZE = 32
    private val cache = LruCache<String, ImageBitmap>(64)

    fun peek(blurHash: String?): ImageBitmap? =
        blurHash?.takeIf { it.isNotBlank() }?.let { cache.get(it) }

    suspend fun get(blurHash: String?): ImageBitmap? {
        if (blurHash.isNullOrBlank()) return null
        cache.get(blurHash)?.let { return it }
        val bitmap = withContext(Dispatchers.Default) {
            BlurHash.decode(blurHash, DECODE_SIZE, DECODE_SIZE)?.asImageBitmap()
        } ?: return null
        cache.put(blurHash, bitmap)
        return bitmap
    }
}
