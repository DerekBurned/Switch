package com.example.messenger.data.local.obx

import io.objectbox.annotation.ConflictStrategy
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Unique

@Entity
data class ObxContactAlias(
    @Id var boxId: Long = 0,
    @Unique(onConflict = ConflictStrategy.REPLACE) var contactId: String = "",
    var name: String = "",
    var updatedAt: Long = 0,
)
