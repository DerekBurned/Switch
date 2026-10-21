package com.example.messenger.presentation.components.auth

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.example.messenger.presentation.components.auth.model.Countries
import com.example.messenger.presentation.components.auth.model.Country
import com.example.messenger.presentation.components.common.MessengerSearchField
import com.example.messenger.presentation.theme.MessengerShapes
import com.example.messenger.presentation.theme.MessengerTheme
import com.example.messenger.presentation.theme.messengerTokens

@Composable
fun CountryCodePicker(
    selected: Country,
    onCountrySelected: (Country) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .height(56.dp)
                .shadow(elevation = 2.dp, shape = shape, clip = false)
                .clip(shape)
                .background(messengerTokens.fieldFill)
                .clickable { menuOpen = true }
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

        CountryDropdownMenu(
            expanded = menuOpen,
            onDismiss = { menuOpen = false },
            onSelect = {
                onCountrySelected(it)
                menuOpen = false
            }
        )
    }
}

private class BelowAnchorPositionProvider(
    private val verticalGapPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = anchorBounds.left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        val y = (anchorBounds.bottom + verticalGapPx).coerceAtMost(maxY)
        return IntOffset(x, y)
    }
}

@Composable
private fun CountryDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Country) -> Unit,
) {
    if (!expanded) return

    var query by remember { mutableStateOf("") }
    val tokens = messengerTokens
    val verticalGapPx = with(LocalDensity.current) { 4.dp.roundToPx() }

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

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
        popupPositionProvider = remember(verticalGapPx) { BelowAnchorPositionProvider(verticalGapPx) },
    ) {
        Surface(
            modifier = Modifier
                .width(280.dp)
                .shadow(elevation = 12.dp, shape = MessengerShapes.menu, clip = false),
            shape = MessengerShapes.menu,
            color = tokens.cardFill,
            border = BorderStroke(1.dp, tokens.panelBorder),
        ) {
            Column {
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    MessengerSearchField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "Search country or code",
                    )
                }

                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(filtered, key = { it.isoCode }) { country ->
                        CountryRow(country = country, onClick = { onSelect(country) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryRow(country: Country, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = country.flagEmoji, fontSize = 22.sp)
                Box(modifier = Modifier.weight(1f)) {
                    Text(
                        text = country.name,
                        color = messengerTokens.textPrimary,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = country.dialCode,
                    color = messengerTokens.textMuted,
                    fontSize = 14.sp,
                )
            }
        },
        onClick = onClick
    )
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CountryCodePickerPreview() {
    MessengerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.padding(16.dp)) {
                CountryCodePicker(
                    selected = Country("US", "United States", "+1", "🇺🇸"),
                    onCountrySelected = {}
                )
            }
        }
    }
}