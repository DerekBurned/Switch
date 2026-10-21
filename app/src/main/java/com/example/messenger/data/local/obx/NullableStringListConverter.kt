package com.example.messenger.data.local.obx

import com.google.gson.reflect.TypeToken
import io.objectbox.converter.PropertyConverter

class NullableStringListConverter : PropertyConverter<List<String?>?, String?> {
    override fun convertToDatabaseValue(entityProperty: List<String?>?): String? =
        obxGson.toJson(entityProperty ?: emptyList<String?>())

    override fun convertToEntityProperty(databaseValue: String?): List<String?> {
        if (databaseValue.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<String?>>() {}.type
        return obxGson.fromJson(databaseValue, type) ?: emptyList()
    }
}
