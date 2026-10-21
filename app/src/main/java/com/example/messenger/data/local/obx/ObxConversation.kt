package com.example.messenger.data.local.obx

import io.objectbox.annotation.ConflictStrategy
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Unique

@Entity
data class ObxConversation(
    @Id var boxId: Long = 0,
    @Unique(onConflict = ConflictStrategy.REPLACE) var uid: String = "",
    @Convert(converter = StringListConverter::class, dbType = String::class)
    var participantIds: List<String> = emptyList(),
    @Convert(converter = StringListConverter::class, dbType = String::class)
    var participantNames: List<String> = emptyList(),
    @Convert(converter = NullableStringListConverter::class, dbType = String::class)
    var participantAvatars: List<String?> = emptyList(),
    var lastMessage: String? = null,
    var lastMessageSenderId: String = "",
    var lastMessageTimestamp: Long = 0,
    var unreadCount: Int = 0,

    var latestMessageText: String? = null,
    var latestMessageSenderId: String = "",
    var latestMessageTimestamp: Long = 0,
   var clearedAtForMe: Long = 0,
)
