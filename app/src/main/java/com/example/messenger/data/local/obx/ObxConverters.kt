package com.example.messenger.data.local.obx

import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.PhoneNumber
import com.example.messenger.domain.model.PhoneVisibility
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.objectbox.converter.PropertyConverter

private val gson = Gson()

class PhoneNumberConverter : PropertyConverter<PhoneNumber?, String?> {
    override fun convertToDatabaseValue(entityProperty: PhoneNumber?): String? =
        entityProperty?.let { gson.toJson(it) }

    override fun convertToEntityProperty(databaseValue: String?): PhoneNumber? =
        databaseValue?.let { gson.fromJson(it, PhoneNumber::class.java) }
}

class StringListConverter : PropertyConverter<List<String>?, String?> {
    override fun convertToDatabaseValue(entityProperty: List<String>?): String? =
        gson.toJson(entityProperty ?: emptyList<String>())

    override fun convertToEntityProperty(databaseValue: String?): List<String> {
        if (databaseValue.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(databaseValue, type) ?: emptyList()
    }
}

class NullableStringListConverter : PropertyConverter<List<String?>?, String?> {
    override fun convertToDatabaseValue(entityProperty: List<String?>?): String? =
        gson.toJson(entityProperty ?: emptyList<String?>())

    override fun convertToEntityProperty(databaseValue: String?): List<String?> {
        if (databaseValue.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<String?>>() {}.type
        return gson.fromJson(databaseValue, type) ?: emptyList()
    }
}

class PhoneVisibilityConverter : PropertyConverter<PhoneVisibility, String> {
    override fun convertToDatabaseValue(entityProperty: PhoneVisibility): String =
        entityProperty.name

    override fun convertToEntityProperty(databaseValue: String?): PhoneVisibility =
        runCatching { PhoneVisibility.valueOf(databaseValue ?: "") }
            .getOrDefault(PhoneVisibility.HIDDEN)
}

class MediaItemListConverter : PropertyConverter<List<MediaItem>, String> {
    private val type = object : TypeToken<List<MediaItem>>() {}.type

    override fun convertToDatabaseValue(entityProperty: List<MediaItem>): String =
        gson.toJson(entityProperty)

    override fun convertToEntityProperty(databaseValue: String?): List<MediaItem> {
        if (databaseValue.isNullOrEmpty()) return emptyList()
        return runCatching { gson.fromJson<List<MediaItem>>(databaseValue, type) }
            .getOrDefault(emptyList())
    }
}
