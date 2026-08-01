package com.example.messenger.data.remote.call

import java.util.zip.CRC32

object CallUids {
    fun fromUserId(userId: String): Int {
        if (userId.isEmpty()) return 0
        val crc = CRC32()
        crc.update(userId.toByteArray(Charsets.UTF_8))
        return (crc.value and 0x7FFFFFFFL).toInt()
    }
}
