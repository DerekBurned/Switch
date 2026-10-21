package com.example.messenger.data.local.obx

data class ClipPayload(
    val url: String = "",
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val fileKey: String? = null,
    val waveform: List<Int> = emptyList(),
)
