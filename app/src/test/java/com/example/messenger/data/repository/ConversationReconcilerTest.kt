package com.example.messenger.data.repository

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class ConversationReconcilerTest {

    @Test
    fun `server set missing a local id marks it stale`() {
        val stale = ConversationReconciler.staleIds(
            local = setOf("a", "b", "c"),
            remote = RemoteConversations.Server(setOf("a", "c")),
        )
        assertThat(stale).isEqualTo(setOf("b"))
    }

    @Test
    fun `empty server set prunes all local`() {
        val stale = ConversationReconciler.staleIds(
            local = setOf("a", "b"),
            remote = RemoteConversations.Server(emptySet()),
        )
        assertThat(stale).isEqualTo(setOf("a", "b"))
    }

    @Test
    fun `unavailable remote never prunes`() {
        val stale = ConversationReconciler.staleIds(
            local = setOf("a", "b"),
            remote = RemoteConversations.Unavailable,
        )
        assertThat(stale).isEmpty()
    }

    @Test
    fun `server superset prunes nothing`() {
        val stale = ConversationReconciler.staleIds(
            local = setOf("a"),
            remote = RemoteConversations.Server(setOf("a", "b")),
        )
        assertThat(stale).isEmpty()
    }

    @Test
    fun `exact match prunes nothing`() {
        val stale = ConversationReconciler.staleIds(
            local = setOf("a", "b"),
            remote = RemoteConversations.Server(setOf("a", "b")),
        )
        assertThat(stale).isEmpty()
    }

    @Test
    fun `empty local is always empty`() {
        val stale = ConversationReconciler.staleIds(
            local = emptySet(),
            remote = RemoteConversations.Server(setOf("a")),
        )
        assertThat(stale).isEmpty()
    }
}
