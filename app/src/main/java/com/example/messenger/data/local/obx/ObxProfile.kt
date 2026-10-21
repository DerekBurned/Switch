package com.example.messenger.data.local.obx

import com.example.messenger.domain.model.PhoneVisibility
import io.objectbox.annotation.ConflictStrategy
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.Unique

@Entity
data class ObxProfile(
    @Id var boxId: Long = 0,
    @Unique(onConflict = ConflictStrategy.REPLACE) var uid: String = "",
    var username: String? = null,
    var email: String? = null,
    @Index var userId: String = "",
    var avatarUrl: String? = null,
    var lastSeen: Long = 0,
    var isOnline: Boolean = false,
    @Convert(converter = PhoneVisibilityConverter::class, dbType = String::class)
    var phoneVisibility: PhoneVisibility = PhoneVisibility.HIDDEN,
    var fcmToken: String? = null,
)
