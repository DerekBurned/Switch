package com.example.messenger.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class DisplayNameTest {

    @Test
    fun `a contact alias wins over the profile name`() {
        assertThat(resolveDisplayName(rawName = "Alice", alias = "Mum")).isEqualTo("Mum")
    }

    @Test
    fun `the profile name is used when there is no alias`() {
        assertThat(resolveDisplayName(rawName = "Alice", alias = null)).isEqualTo("Alice")
    }

    @Test
    fun `a blank alias falls through to the profile name`() {
        assertThat(resolveDisplayName(rawName = "Alice", alias = "   ")).isEqualTo("Alice")
    }

    @Test
    fun `a leading plus is stripped from a raw phone number`() {
        assertThat(resolveDisplayName(rawName = "+15550100", alias = null)).isEqualTo("15550100")
    }

    @Test
    fun `a missing name falls back to Unknown`() {
        assertThat(resolveDisplayName(rawName = null, alias = null)).isEqualTo("Unknown")
        assertThat(resolveDisplayName(rawName = "  ", alias = null)).isEqualTo("Unknown")
    }

    @Test
    fun `a deleted account overrides both the alias and the name`() {
        assertThat(resolveDisplayName(rawName = "Alice", alias = "Mum", deleted = true))
            .isEqualTo(DELETED_USER_NAME)
    }
}
