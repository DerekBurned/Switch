package com.example.messenger.data.local.obx

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class ObxSyncQueueItem(
    @Id var boxId: Long = 0,
    var entityType: String = "",
    var entityId: String = "",
    var action: String = "",
    var timestamp: Long = 0,
    var retryCount: Int = 0,
)
