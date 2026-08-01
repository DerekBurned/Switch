package com.example.messenger.data.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.presentation.state.GalleryFilter
import com.example.messenger.presentation.state.GalleryItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreGallery @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend fun load(filter: GalleryFilter): List<GalleryItem> = withContext(Dispatchers.IO) {
        val items = buildList {
            if (filter != GalleryFilter.VIDEOS) addAll(queryImages())
            if (filter != GalleryFilter.PHOTOS) addAll(queryVideos())
        }.sortedByDescending { it.dateAdded }
        Log.d(TAG, "load filter=$filter count=${items.size}")
        items
    }

    private fun queryImages(): List<GalleryItem> {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
        )
        return runCatching {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                buildList {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        add(
                            GalleryItem(
                                id = id,
                                uri = ContentUris.withAppendedId(collection, id),
                                kind = MediaItem.IMAGE,
                                durationMs = 0L,
                                dateAdded = cursor.getLong(dateCol),
                            ),
                        )
                    }
                }
            }.orEmpty()
        }.onFailure { Log.w(TAG, "queryImages failed", it) }.getOrDefault(emptyList())
    }

    private fun queryVideos(): List<GalleryItem> {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
        )
        return runCatching {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                buildList {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        add(
                            GalleryItem(
                                id = id,
                                uri = ContentUris.withAppendedId(collection, id),
                                kind = MediaItem.VIDEO,
                                durationMs = cursor.getLong(durationCol),
                                dateAdded = cursor.getLong(dateCol),
                            ),
                        )
                    }
                }
            }.orEmpty()
        }.onFailure { Log.w(TAG, "queryVideos failed", it) }.getOrDefault(emptyList())
    }

    private companion object {
        const val TAG = "MediaStoreGallery"
    }
}
