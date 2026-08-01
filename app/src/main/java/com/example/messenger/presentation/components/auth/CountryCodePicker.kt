package com.example.messenger.presentation.components.auth
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CountryCodePicker(
    selected: Country,
    onCountrySelected: (Country) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialogOpen by remember { mutableStateOf(false) }

    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .height(56.dp)
            .shadow(elevation = 2.dp, shape = shape, clip = false)
            .clip(shape)
            .background(messengerTokens.fieldFill)
            .clickable { dialogOpen = true }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = selected.flagEmoji, fontSize = 20.sp)
        Text(
            text = selected.dialCode,
            color = messengerTokens.textOnField,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Choose country",
            tint = messengerTokens.textMuted,
        )
    }

    if (dialogOpen) {
        CountryPickerDialog(
            onDismiss = { dialogOpen = false },
            onSelect = {
                onCountrySelected(it)
                dialogOpen = false
            },
        )
    }
}

@Composable
private fun CountryPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (Country) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim()
        if (q.isBlank()) {
            Countries.all
        } else {
            Countries.all.filter { country ->
                country.name.contains(q, ignoreCase = true) ||
                    country.dialCode.contains(q) ||
                    country.isoCode.contains(q, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Select country") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search country or code") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(filtered, key = { it.isoCode }) { country ->
                        CountryRow(country = country, onClick = { onSelect(country) })
                    }
                }
            }
        },
    )
}

@Composable
private fun CountryRow(country: Country, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = country.flagEmoji, fontSize = 22.sp)
        Box(modifier = Modifier.weight(1f)) {
            Text(
                text = country.name,
                color = Color.Black,
                fontSize = 15.sp,
            )
        }
        Text(
            text = country.dialCode,
            color = Color.Gray,
            fontSize = 14.sp,
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CountryCodePickerPreview() {
    MessengerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.padding(16.dp)) {
                CountryCodePicker(selected = Countries.default, onCountrySelected = {})
            }
        }
    }
}
