package com.example.messenger.util

fun resolveDisplayName(rawName: String?, alias: String?): String {
    alias?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
    val cleaned = rawName?.trim()?.removePrefix("+")?.trim()
    return cleaned?.takeIf { it.isNotBlank() } ?: "Unknown"
}
