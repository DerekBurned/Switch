package com.example.messenger.data.local.obx

import io.objectbox.annotation.ConflictStrategy
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Unique

@Entity
data class ObxRecentContact(
    @Id var boxId: Long = 0,
    @Unique(onConflict = ConflictStrategy.REPLACE) var userId: String = "",
    var username: String = "",
    var avatarUrl: String? = null,
    var lastSearchedAt: Long = 0,
    var lastOpenedAt: Long = 0,
)
