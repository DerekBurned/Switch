package com.example.messenger.data.local.obx

import com.example.messenger.domain.model.MediaItem
import com.google.gson.reflect.TypeToken
import io.objectbox.converter.PropertyConverter

class MediaItemListConverter : PropertyConverter<List<MediaItem>, String> {
    private val type = object : TypeToken<List<MediaItem>>() {}.type

    override fun convertToDatabaseValue(entityProperty: List<MediaItem>): String =
        obxGson.toJson(entityProperty)

    override fun convertToEntityProperty(databaseValue: String?): List<MediaItem> {
        if (databaseValue.isNullOrEmpty()) return emptyList()
        return runCatching { obxGson.fromJson<List<MediaItem>>(databaseValue, type) }
            .getOrDefault(emptyList())
    }
}
