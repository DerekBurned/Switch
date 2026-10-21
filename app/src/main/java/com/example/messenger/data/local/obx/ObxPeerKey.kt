package com.example.messenger.data.local.obx

import io.objectbox.annotation.ConflictStrategy
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.Unique

@Entity
data class ObxPeerKey(
    @Id var boxId: Long = 0,
    @Unique(onConflict = ConflictStrategy.REPLACE) var uidEpoch: String = "",
    @Index var uid: String = "",
    var epoch: Int = 0,
    var publicKey: String = "",
    var verified: Boolean = false,
    var seenAt: Long = 0,
)
