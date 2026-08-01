package com.example.messenger.presentation.components.common
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Surface
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.messenger.presentation.screens.ui.theme.Dimens
import com.example.messenger.presentation.screens.ui.theme.MessengerShapes
import com.example.messenger.presentation.screens.ui.theme.messengerTokens

@Composable
fun MessengerSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
) {
    val tokens = messengerTokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(MessengerShapes.tab)
            .background(tokens.trackFill)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = tokens.textPrimary.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = tokens.textPrimary.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = tokens.textPrimary),
                cursorBrush = SolidColor(tokens.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear",
                tint = tokens.textPrimary.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onValueChange("") },
            )
        }
    }
}

@Composable
fun MessengerInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    shape: Shape = MessengerShapes.field,
) {
    val tokens = messengerTokens
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.fieldHeight)
            .clip(shape)
            .background(tokens.fieldFill)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = tokens.textMuted,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = tokens.textOnField),
            cursorBrush = SolidColor(tokens.accent),
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun OtpCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    ringProgress: () -> Float,
    expired: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Enter code",
) {
    val tokens = messengerTokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.fieldHeight)
            .clip(MessengerShapes.field)
            .background(tokens.fieldFill)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = tokens.textMuted,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = tokens.textOnField),
                cursorBrush = SolidColor(tokens.accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        if (expired) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Resend code",
                tint = tokens.accent,
                modifier = Modifier
                    .size(22.dp)
                    .clip(MessengerShapes.tab)
                    .clickable { onRetry() },
            )
        } else {
            CircularProgressIndicator(
                progress = ringProgress,
                color = tokens.accent,
                trackColor = tokens.textMuted.copy(alpha = 0.25f),
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MessengerTextFieldsPreview() {
    MessengerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MessengerSearchField(value = "", onValueChange = {})
                MessengerSearchField(value = "Alice", onValueChange = {})
                MessengerInputField(value = "", onValueChange = {}, placeholder = "Enter phone number")
                MessengerInputField(value = "Designer", onValueChange = {}, placeholder = "Bio")
            }
        }
    }
}
