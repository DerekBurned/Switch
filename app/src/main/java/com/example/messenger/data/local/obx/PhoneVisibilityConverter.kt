package com.example.messenger.data.local.obx

import com.example.messenger.domain.model.PhoneVisibility
import io.objectbox.converter.PropertyConverter

class PhoneVisibilityConverter : PropertyConverter<PhoneVisibility, String> {
    override fun convertToDatabaseValue(entityProperty: PhoneVisibility): String =
        entityProperty.name

    override fun convertToEntityProperty(databaseValue: String?): PhoneVisibility =
        runCatching { PhoneVisibility.valueOf(databaseValue ?: "") }
            .getOrDefault(PhoneVisibility.HIDDEN)
}
