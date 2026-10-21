package com.example.messenger.presentation.components.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.model.User
import com.example.messenger.presentation.base.ObserveAsEvents
import com.example.messenger.presentation.components.common.MessengerAvatar
import com.example.messenger.presentation.components.common.WallpaperBackground
import com.example.messenger.presentation.featureflag.featureEnabled
import com.example.messenger.presentation.theme.LocalTabBarPosition
import com.example.messenger.presentation.theme.PrimaryBlue
import com.example.messenger.presentation.theme.messengerTokens
import com.example.messenger.presentation.theme.model.BarPosition
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SearchResultsOverlay(
    query: String,
    onConversationCreated: (conversationId: String, partnerId: String, partnerName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchUsersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val tokens = messengerTokens

    LaunchedEffect(query) {
        delay(300)
        viewModel.searchUsers(query)
    }

    ObserveAsEvents(viewModel.effect) { effect ->
        when (effect) {
            is SearchUsersEffect.ConversationCreated -> {
                val conversation = effect.conversation
                val myUid = FirebaseAuth.getInstance().currentUser?.uid
                val idx = conversation.participantIds.indexOfFirst { it != myUid }
                val partnerId = conversation.participantIds.getOrNull(idx) ?: ""
                val partnerName = conversation.participantNames.getOrNull(idx)
                    ?.takeIf { it.isNotBlank() } ?: "Unknown"
                onConversationCreated(conversation.id, partnerId, partnerName)
            }
        }
    }

    val barAtBottom = LocalTabBarPosition.current == BarPosition.BOTTOM
    WallpaperBackground(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(
                    top = if (barAtBottom) 8.dp else 72.dp,
                    bottom = if (barAtBottom) 72.dp else 8.dp,
                ),
        ) {
            when {
                uiState.isCreatingConversation || uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryBlue,
                    )
                }
                uiState.error != null -> CenterMessage(uiState.error!!.asString())
                query.isBlank() -> {
                    val recentEnabled = featureEnabled(FeatureFlag.SEARCH_RECENT)
                    val searched = if (recentEnabled) uiState.recentSearched else emptyList()
                    val searchedIds = searched.mapTo(HashSet()) { it.id }
                    val opened = if (recentEnabled) uiState.recentOpened.filterNot { it.id in searchedIds } else emptyList()
                    if (searched.isEmpty() && opened.isEmpty()) {
                        CenterMessage("Type a username to search")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                        ) {
                            if (searched.isNotEmpty()) {
                                item(key = "header_searched") { SectionHeader("Recently searched") }
                                items(searched, key = { "s_${it.id}" }) { user ->
                                    SearchResultRow(
                                        user = user,
                                        onClick = { viewModel.createConversationWithUser(user) },
                                    )
                                }
                            }
                            if (opened.isNotEmpty()) {
                                item(key = "header_opened") { SectionHeader("Recently opened") }
                                items(opened, key = { "o_${it.id}" }) { user ->
                                    SearchResultRow(
                                        user = user,
                                        onClick = { viewModel.createConversationWithUser(user) },
                                    )
                                }
                            }
                        }
                    }
                }
                uiState.users.isEmpty() -> CenterMessage("No users found")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                ) {
                    items(uiState.users, key = { it.id }) { user ->
                        SearchResultRow(user = user, onClick = { viewModel.createConversationWithUser(user) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    val tokens = messengerTokens
    Text(
        text = text,
        color = tokens.textMuted,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun BoxScope.CenterMessage(text: String) {
    val tokens = messengerTokens
    Text(
        text = text,
        color = tokens.textPrimary.copy(alpha = 0.6f),
        modifier = Modifier.align(Alignment.Center).padding(16.dp),
    )
}

@Composable
private fun SearchResultRow(user: User, onClick: () -> Unit) {
    val tokens = messengerTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MessengerAvatar(name = user.username.orEmpty(), photoUrl = user.avatarUrl, size = 48.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = user.username ?: "Unknown",
                color = tokens.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            val secondary = user.email ?: user.phoneNumber?.getFullNumber()
            if (!secondary.isNullOrBlank()) {
                Text(
                    text = secondary,
                    color = tokens.textPrimary.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                )
            }
        }
    }
}
