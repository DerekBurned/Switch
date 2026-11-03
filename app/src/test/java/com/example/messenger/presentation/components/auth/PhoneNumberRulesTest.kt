package com.example.messenger.presentation.components.auth

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.example.messenger.presentation.components.auth.model.Countries
import com.example.messenger.presentation.components.auth.model.Country
import com.google.i18n.phonenumbers.PhoneNumberUtil
import org.junit.jupiter.api.Test

class PhoneNumberRulesTest {

    private val rules = PhoneNumberRules()
    private val util = PhoneNumberUtil.getInstance()

    private fun country(iso: String): Country = Countries.all.first { it.isoCode == iso }

    @Test
    fun `every listed country is known to libphonenumber with a matching dial code`() {
        val mismatched = Countries.all.filterNot { country ->
            val code = util.getCountryCodeForRegion(country.isoCode)
            code != 0 && country.dialCode.removePrefix("+").startsWith(code.toString())
        }
        assertThat(mismatched).isEmpty()
    }

    @Test
    fun `us numbers are capped at ten digits and validated`() {
        val us = country("US")
        assertThat(rules.sanitize(us, "650253000099")).isEqualTo("6502530000")
        assertThat(rules.isValid(us, "6502530000")).isTrue()
        assertThat(rules.isValid(us, "650253000")).isFalse()
        assertThat(rules.toE164(us, "6502530000")).isEqualTo("+16502530000")
        assertThat(rules.toE164(us, "16502530000")).isEqualTo("+16502530000")
    }

    @Test
    fun `a typed trunk prefix gets one extra digit and is dropped from the e164 number`() {
        val gb = country("GB")
        assertThat(rules.maxLength(gb, "7400")).isEqualTo(10)
        assertThat(rules.maxLength(gb, "07400")).isEqualTo(11)
        assertThat(rules.sanitize(gb, "074001234567")).isEqualTo("07400123456")
        assertThat(rules.toE164(gb, "07400123456")).isEqualTo("+447400123456")
        assertThat(rules.toE164(gb, "7400123456")).isEqualTo("+447400123456")
    }

    @Test
    fun `dial codes that include an area code leave only the local digits`() {
        val bahamas = country("BS")
        val example = util.getNationalSignificantNumber(util.getExampleNumber("BS"))
        val local = example.removePrefix("242")
        assertThat(rules.maxLength(bahamas, "")).isEqualTo(7)
        assertThat(rules.isValid(bahamas, local)).isTrue()
        assertThat(rules.toE164(bahamas, local)).isEqualTo("+1$example")
    }

    @Test
    fun `formatting and letters are stripped`() {
        assertThat(rules.sanitize(country("US"), "(650) 253-0000")).isEqualTo("6502530000")
        assertThat(rules.sanitize(country("US"), "abc")).isEqualTo("")
    }

    @Test
    fun `an invalid number has no e164 form`() {
        assertThat(rules.toE164(country("US"), "1234567890")).isNull()
        assertThat(rules.toE164(country("US"), "")).isNull()
    }
}
