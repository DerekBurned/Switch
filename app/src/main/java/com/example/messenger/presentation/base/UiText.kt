package com.example.messenger.presentation.base

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

sealed interface UiText {
    data class DynamicString(val value: String) : UiText
    class StringResource(
        @StringRes val id: Int,
        vararg val args: Any,
    ) : UiText

    fun asString(context: Context): String = when (this) {
        is DynamicString -> value
        is StringResource -> context.getString(id, *args)
    }

    @Composable
    @ReadOnlyComposable
    fun asString(): String = when (this) {
        is DynamicString -> value
        is StringResource -> stringResource(id, *args)
    }
}

fun String.toUiText(): UiText = UiText.DynamicString(this)

fun Int.toUiText(vararg args: Any): UiText = UiText.StringResource(this, *args)
