package com.example.messenger.data.local.obx

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.example.messenger.data.local.obx.mappers.toDomain
import com.example.messenger.data.local.obx.mappers.toObx
import com.example.messenger.domain.model.User
import org.junit.jupiter.api.Test

class ObxUserMapperTest {

    @Test
    fun `a live user round trips without a tombstone`() {
        val user = User(id = "u1", username = "Alice", usernameLower = "alice")

        val restored = user.toObx().toDomain()

        assertThat(restored.id).isEqualTo("u1")
        assertThat(restored.username).isEqualTo("Alice")
        assertThat(restored.deleted).isFalse()
        assertThat(restored.deletedAt).isEqualTo(0L)
    }

    @Test
    fun `a tombstone survives the round trip`() {
        val user = User(id = "u1", username = "Alice", deleted = true, deletedAt = 1_700_000_000_000L)

        val restored = user.toObx().toDomain()

        assertThat(restored.deleted).isTrue()
        assertThat(restored.deletedAt).isEqualTo(1_700_000_000_000L)
    }

    @Test
    fun `a tombstoned profile with no username still maps`() {
        val user = User(id = "u1", username = null, deleted = true, deletedAt = 5L)

        val row = user.toObx()

        assertThat(row.deleted).isTrue()
        assertThat(row.username).isEqualTo("Unknown")
    }
}
