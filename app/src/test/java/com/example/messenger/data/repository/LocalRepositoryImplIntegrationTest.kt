package com.example.messenger.data.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.example.messenger.data.local.obx.ObxContactAlias
import com.example.messenger.data.local.obx.ObxConversation
import com.example.messenger.data.local.obx.ObxMessage
import com.example.messenger.data.local.obx.ObxSyncQueueItem
import com.example.messenger.data.local.obx.ObxUser
import com.example.messenger.testutil.ObjectBoxExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class LocalRepositoryImplIntegrationTest {

    @JvmField
    @RegisterExtension
    val obx = ObjectBoxExtension()

    @Test
    fun `resetDB empties every box`() = runTest {
        obx.box<ObxMessage>().put(ObxMessage(uid = "m1"))
        obx.box<ObxConversation>().put(ObxConversation(uid = "c1"))
        obx.box<ObxUser>().put(ObxUser(uid = "u1"))
        obx.box<ObxContactAlias>().put(ObxContactAlias(contactId = "u1", name = "U"))
        obx.box<ObxSyncQueueItem>().put(ObxSyncQueueItem(entityId = "m1"))

        LocalRepositoryImpl(obx.store).resetDB()

        assertThat(obx.box<ObxMessage>().count()).isEqualTo(0L)
        assertThat(obx.box<ObxConversation>().count()).isEqualTo(0L)
        assertThat(obx.box<ObxUser>().count()).isEqualTo(0L)
        assertThat(obx.box<ObxContactAlias>().count()).isEqualTo(0L)
        assertThat(obx.box<ObxSyncQueueItem>().count()).isEqualTo(0L)
    }
}
