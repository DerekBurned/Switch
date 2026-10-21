package com.example.messenger.data.media

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class MediaCacheTemporariesTest {

    @Test
    fun `recordings and partial files are temporaries`() {
        listOf(
            "recording_1700000000000.m4a",
            "m1.m4a.4821.dl",
            "m1.7731.enc.dl",
            "m1.m4a.9137.part",
            "m1.enc",
        ).forEach { name ->
            assertThat(isStaleTemporaryName(name), name).isTrue()
        }
    }

    @Test
    fun `cached clips photos and videos are never temporaries`() {
        listOf("m1.m4a", "m1.mp4", "item.jpg").forEach { name ->
            assertThat(isStaleTemporaryName(name), name).isFalse()
        }
    }
}
