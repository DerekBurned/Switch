package com.example.messenger.presentation.components.media

import android.content.Context
import com.example.messenger.data.media.MediaCache
import com.example.messenger.domain.model.MediaItem
import java.io.File

fun mediaCacheFile(context: Context, item: MediaItem): File =
    File(File(context.cacheDir, MediaCache.DIR), MediaCache.fileName(item.id, item.kind))
