package com.example.messenger.data.local.obx.mappers

import com.example.messenger.data.local.obx.ObxUser
import com.example.messenger.domain.model.PhoneVisibility
import com.example.messenger.domain.model.User

fun ObxUser.toDomain(): User = User(
    id = uid,
    username = username,
    usernameLower = usernameLower.ifBlank { username.lowercase() },
    email = email,
    phoneNumber = phoneNumber,
    avatarUrl = avatarUrl,
    lastSeen = lastSeen,
    isOnline = isOnline,
    phoneVisibility = PhoneVisibility.HIDDEN,
    fcmToken = null,
    deleted = deleted,
    deletedAt = deletedAt,
)

fun User.toObx(): ObxUser = ObxUser(
    uid = id,
    username = username ?: "Unknown",
    usernameLower = (usernameLower ?: username)?.lowercase().orEmpty(),
    email = email,
    phoneNumber = phoneNumber,
    avatarUrl = avatarUrl,
    lastSeen = lastSeen,
    isOnline = isOnline,
    deleted = deleted,
    deletedAt = deletedAt,
)