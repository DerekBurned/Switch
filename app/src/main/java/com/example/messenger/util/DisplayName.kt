package com.example.messenger.util

const val DELETED_USER_NAME = "Unknown user"

fun resolveDisplayName(rawName: String?, alias: String?, deleted: Boolean = false): String {
    if (deleted) return DELETED_USER_NAME
    alias?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
    val cleaned = rawName?.trim()?.removePrefix("+")?.trim()
    return cleaned?.takeIf { it.isNotBlank() } ?: "Unknown"
}
