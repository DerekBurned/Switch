package com.example.messenger.presentation.components.auth

import com.example.messenger.presentation.components.auth.model.Country
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import com.google.i18n.phonenumbers.PhoneNumberUtil.ValidationResult
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import java.util.concurrent.ConcurrentHashMap

class PhoneNumberRules(private val util: PhoneNumberUtil = PhoneNumberUtil.getInstance()) {

    private val maxSignificantLengths = ConcurrentHashMap<Int, Int>()

    fun sanitize(country: Country, input: String): String {
        val digits = input.filter { it.isDigit() }
        return digits.take(maxLength(country, digits))
    }

    fun maxLength(country: Country, digits: String): Int {
        val countryCode = util.getCountryCodeForRegion(country.isoCode)
        if (countryCode == 0) return MAX_SIGNIFICANT_DIGITS
        val dialDigits = country.dialCode.filter { it.isDigit() }
        val areaDigits = dialDigits.length - countryCode.toString().length
        val trunk = if (areaDigits == 0) util.getNddPrefixForRegion(country.isoCode, true).orEmpty() else ""
        val trunkDigits = if (trunk.isNotEmpty() && digits.startsWith(trunk)) trunk.length else 0
        return maxSignificantLength(countryCode) - areaDigits + trunkDigits
    }

    fun isValid(country: Country, digits: String): Boolean = validNumber(country, digits) != null

    fun toE164(country: Country, digits: String): String? =
        validNumber(country, digits)?.let { util.format(it, PhoneNumberFormat.E164) }

    private fun validNumber(country: Country, digits: String): PhoneNumber? =
        parse(country, digits)?.takeIf { util.isValidNumberForRegion(it, country.isoCode) }

    private fun parse(country: Country, digits: String): PhoneNumber? {
        if (util.getCountryCodeForRegion(country.isoCode) == 0 || digits.isEmpty()) return null
        val dialDigits = country.dialCode.filter { it.isDigit() }
        return try {
            util.parse("+$dialDigits$digits", country.isoCode)
        } catch (_: NumberParseException) {
            null
        }
    }

    private fun maxSignificantLength(countryCode: Int): Int = maxSignificantLengths.getOrPut(countryCode) {
        (MAX_SIGNIFICANT_DIGITS downTo 1).firstOrNull { length ->
            val probe = PhoneNumber()
                .setCountryCode(countryCode)
                .setNationalNumber("5".repeat(length).toLong())
            util.isPossibleNumberWithReason(probe) != ValidationResult.TOO_LONG
        } ?: MAX_SIGNIFICANT_DIGITS
    }

    private companion object {
        const val MAX_SIGNIFICANT_DIGITS = 17
    }
}
