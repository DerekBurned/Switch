package com.example.messenger.util

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 8
                && password.any { it.isDigit() }
                && password.any { it.isLetter() }
    }

    fun isValidPhoneNumber(phone: String): Boolean {
        val cleaned = phone.replace("[\\s\\-()]".toRegex(), "")
        return cleaned.matches("^\\+?[1-9]\\d{7,14}$".toRegex())
    }

    fun isValidUsername(username: String): Boolean {
        return username.length in 3..30
                && username.matches("^[a-zA-Z0-9_]+$".toRegex())
    }
}
