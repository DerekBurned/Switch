package com.example.messenger.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.graphics.scale
import com.example.messenger.domain.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class MediaMetadata @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    data class Info(
        val width: Int,
        val height: Int,
        val durationMs: Long,
        val blurHash: String,
    )

    fun extract(uri: Uri, kind: String): Info =
        if (kind == MediaItem.VIDEO) extractVideo(uri) else extractImage(uri)

    private fun extractImage(uri: Uri): Info {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val width = bounds.outWidth.coerceAtLeast(0)
        val height = bounds.outHeight.coerceAtLeast(0)

        val sample = sampleSize(width, height)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val small = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        }
        val hash = small?.let { BlurHash.encode(scaleForBlur(it)) }.orEmpty()
        small?.recycle()
        return Info(width, height, 0L, hash)
    }

    private fun extractVideo(uri: Uri): Info {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val width = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val duration = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val frame = retriever.getFrameAtTime(0)
            val hash = frame?.let { BlurHash.encode(scaleForBlur(it)) }.orEmpty()
            frame?.recycle()
            Info(width, height, duration, hash)
        } catch (e: Exception) {
            Info(0, 0, 0L, "")
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        var dim = max(width, height)
        while (dim > BLUR_SOURCE_MAX * 4) {
            sample *= 2
            dim /= 2
        }
        return sample
    }

    private fun scaleForBlur(src: Bitmap): Bitmap {
        val maxDim = max(src.width, src.height)
        if (maxDim <= BLUR_SOURCE_MAX) return src
        val scale = BLUR_SOURCE_MAX.toFloat() / maxDim
        val w = (src.width * scale).toInt().coerceAtLeast(1)
        val h = (src.height * scale).toInt().coerceAtLeast(1)
        return src.scale(w, h)
    }

    private companion object {
        const val BLUR_SOURCE_MAX = 32
    }
}
