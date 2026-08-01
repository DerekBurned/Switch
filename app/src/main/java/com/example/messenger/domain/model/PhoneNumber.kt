package com.example.messenger.domain.model

data class PhoneNumber(
    val countryCode: String = "",
    val number: String = "",
){
    fun getFullNumber(): String {
        return countryCode + number
    }
}