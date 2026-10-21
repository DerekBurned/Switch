package com.example.messenger.data.local.obx

import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.MessageStatus
import io.objectbox.annotation.ConflictStrategy
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.Unique

@Entity
data class ObxMessage(
    @Id var boxId: Long = 0,
    @Unique(onConflict = ConflictStrategy.REPLACE) var uid: String = "",
    @Index var conversationId: String = "",
    var senderId: String = "",
    var text: String = "",
    var timestamp: Long = 0,
    var status: String = MessageStatus.SENT.name,
    var isRead: Boolean = false,
    var deleted: Boolean = false,

    @Index var type: String = com.example.messenger.domain.model.Message.TYPE_TEXT,

    var replyToMessageId: String? = null,
    var replyToText: String? = null,
    var replyToSenderId: String? = null,
    var callDurationSeconds: Int = 0,
    var callVideo: Boolean = false,

    @Convert(converter = MediaItemListConverter::class, dbType = String::class)
    var mediaItemsJson: List<MediaItem> = emptyList(),

    @Convert(converter = ClipConverter::class, dbType = String::class)
    var clipJson: ClipPayload? = null,

    var clipPlayed: Boolean = false,
)
