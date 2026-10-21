package com.example.messenger.data.local.obx

import com.example.messenger.domain.model.PhoneNumber
import io.objectbox.converter.PropertyConverter

class PhoneNumberConverter : PropertyConverter<PhoneNumber?, String?> {
    override fun convertToDatabaseValue(entityProperty: PhoneNumber?): String? =
        entityProperty?.let { obxGson.toJson(it) }

    override fun convertToEntityProperty(databaseValue: String?): PhoneNumber? =
        databaseValue?.let { obxGson.fromJson(it, PhoneNumber::class.java) }
}
