package com.example.messenger.presentation.screens.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.messenger.domain.model.User
import com.example.messenger.presentation.components.list.ContactRow
import com.example.messenger.presentation.theme.MessengerTheme
import com.example.messenger.presentation.theme.OnSurface
import com.example.messenger.presentation.theme.OnSurfaceMuted
import com.example.messenger.presentation.theme.PrimaryBlue

@Composable
internal fun ContactsScreenContentInner(
    modifier: Modifier = Modifier,
    users: List<User>,
    isLoading: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onContactClick: (User) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val shape = RoundedCornerShape(12.dp)
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .shadow(elevation = 2.dp, shape = shape, clip = false),
            placeholder = { Text("Search contacts", color = OnSurfaceMuted) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = OnSurfaceMuted) },
            singleLine = true,
            shape = shape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = PrimaryBlue,
            ),
        )

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }
            users.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (query.isBlank()) {
                            "Start typing to find contacts"
                        } else {
                            "No contacts found"
                        },
                        color = Color.Gray,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    items(users) { user ->
                        ContactRow(user = user, onClick = { onContactClick(user) })
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ContactsScreenContentPreview() {
    MessengerTheme {
        ContactsScreenContentInner(
            users = listOf(
                User(id = "1", username = "Alice", email = "alice@example.com"),
                User(id = "2", username = "Bob", email = "bob@example.com"),
                User(id = "3", username = "Carol", email = "carol@example.com"),
            ),
            isLoading = false,
            query = "",
            onQueryChange = {},
            onContactClick = {},
        )
    }
}
