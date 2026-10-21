package com.example.messenger.data.remote.dto.message

import com.example.messenger.domain.model.MediaItem

data class RemoteMediaItemDto(
    val id: String = "",
    val kind: String = "IMAGE",
    val url: String = "",
    val storagePath: String = "",
    val blurHash: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
)

fun RemoteMediaItemDto.toMediaItem(): MediaItem = MediaItem(
    id = id,
    kind = kind,
    url = url,
    storagePath = storagePath,
    blurHash = blurHash,
    width = width,
    height = height,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
)
fun MediaItem.toRemoteDto(): RemoteMediaItemDto = RemoteMediaItemDto(
    id = id,
    kind = kind,
    url = url,
    storagePath = storagePath,
    blurHash = blurHash,
    width = width,
    height = height,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
)