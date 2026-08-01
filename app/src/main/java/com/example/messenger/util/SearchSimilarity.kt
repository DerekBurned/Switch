package com.example.messenger.util

fun similarityScore(query: String, candidate: String): Double {
    val q = query.trim().lowercase()
    val c = candidate.trim().lowercase()
    if (q.isEmpty() || c.isEmpty()) return 0.0
    return when {
        c == q -> 1.0
        c.startsWith(q) -> 0.9
        c.contains(q) -> 0.75
        else -> {
            val maxLen = maxOf(q.length, c.length)
            val norm = 1.0 - levenshtein(q, c).toDouble() / maxLen
            norm.coerceIn(0.0, 1.0) * 0.7
        }
    }
}

private fun levenshtein(a: String, b: String): Int {
    val prev = IntArray(b.length + 1) { it }
    val curr = IntArray(b.length + 1)
    for (i in 1..a.length) {
        curr[0] = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            curr[j] = minOf(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
        }
        System.arraycopy(curr, 0, prev, 0, curr.size)
    }
    return prev[b.length]
}
