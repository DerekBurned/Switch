package com.example.messenger.data.local.obx

import io.objectbox.converter.PropertyConverter

class ClipConverter : PropertyConverter<ClipPayload?, String?> {

    override fun convertToDatabaseValue(entityProperty: ClipPayload?): String? =
        entityProperty?.let { obxGson.toJson(it) }

    override fun convertToEntityProperty(databaseValue: String?): ClipPayload? {
        if (databaseValue.isNullOrEmpty()) return null
        return runCatching { obxGson.fromJson(databaseValue, ClipPayload::class.java) }.getOrNull()
    }
}
