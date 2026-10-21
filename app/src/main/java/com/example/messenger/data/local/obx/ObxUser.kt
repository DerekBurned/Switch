package com.example.messenger.data.local.obx

import com.example.messenger.domain.model.PhoneNumber
import io.objectbox.annotation.ConflictStrategy
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.Unique

@Entity
data class ObxUser(
    @Id var boxId: Long = 0,
    @Unique(onConflict = ConflictStrategy.REPLACE) var uid: String = "",
    @Index var username: String = "",
    @Index var usernameLower: String = "",
    @Index var email: String? = null,
    @Convert(converter = PhoneNumberConverter::class, dbType = String::class)
    var phoneNumber: PhoneNumber? = null,
    var avatarUrl: String? = null,
    var lastSeen: Long = 0,
    var isOnline: Boolean = false,
    var deleted: Boolean = false,
    var deletedAt: Long = 0,
)
