package com.example.messenger.presentation.screens
import com.example.messenger.presentation.components.list.ForwardConversationRow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.domain.model.Conversation
import com.example.messenger.presentation.screens.ui.theme.LightGray
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme
import com.example.messenger.presentation.screens.ui.theme.PrimaryBlue
import com.example.messenger.presentation.state.ForwardMessageUiState
import com.example.messenger.presentation.viewmodel.ForwardMessageViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardMessageBottomSheet(
    onDismiss: () -> Unit,
    onForward: (conversationId: String) -> Unit,
    viewModel: ForwardMessageViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    ForwardMessageBottomSheetContent(
        state = state,
        currentUserId = currentUserId,
        onDismiss = onDismiss,
        onQueryChange = viewModel::onQueryChange,
        onSearchActiveChange = viewModel::setSearchActive,
        onForward = { id ->
            onForward(id)
            onDismiss()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForwardMessageBottomSheetContent(
    state: ForwardMessageUiState,
    currentUserId: String,
    onDismiss: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onForward: (String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryBlue)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (state.searchActive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        TextField(
                            value = state.query,
                            onValueChange = onQueryChange,
                            placeholder = { Text("Search...", color = Color.White.copy(0.7f)) },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color.White,
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        )
                        IconButton(onClick = { onSearchActiveChange(false) }) {
                            Icon(Icons.Default.Close, contentDescription = "Close search", tint = Color.White)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Send to", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { onSearchActiveChange(true) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
            ) {
                val list = state.filteredConversations
                if (list.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (state.isLoading) "Loading..." else "No conversations",
                                color = Color.Gray,
                                fontSize = 14.sp,
                            )
                        }
                    }
                } else {
                    items(list, key = { it.id }) { conv ->
                        ForwardConversationRow(
                            conversation = conv,
                            currentUserId = currentUserId,
                            onClick = { onForward(conv.id) },
                        )
                        HorizontalDivider(color = LightGray, thickness = 0.5.dp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun ForwardMessageBottomSheetPreview() {
    MessengerTheme {
        
        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
            Text("ForwardMessageBottomSheet (preview stub)", modifier = Modifier.padding(16.dp))
        }
    }
}
