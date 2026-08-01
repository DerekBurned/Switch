package com.example.messenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.messenger.data.repository.CallHistoryRepository
import com.example.messenger.domain.model.CallHistoryEntry
import com.example.messenger.domain.repository.IConversationRepository
import com.example.messenger.domain.repository.IMessageRepository
import com.example.messenger.domain.repository.IUserRepository
import com.example.messenger.util.resolveDisplayName
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallsViewModel @Inject constructor(
    callHistoryRepository: CallHistoryRepository,
    private val userRepository: IUserRepository,
    private val conversationRepository: IConversationRepository,
    private val messageRepository: IMessageRepository,
    firebaseAuth: FirebaseAuth,
) : ViewModel() {

    private val avatars = MutableStateFlow<Map<String, String>>(emptyMap())
    private val fetchedIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            runCatching {
                val conversations = conversationRepository.getAllConversations().first()
                conversations
                    .sortedByDescending { it.lastMessageTimestamp }
                    .take(20)
                    .forEach { conversation ->
                        messageRepository.backfillRecentMessages(conversation.id)
                    }
            }.onFailure { android.util.Log.w("CallsVM", "call history backfill failed", it) }
        }
    }

    private val history = callHistoryRepository
        .getCallHistory(firebaseAuth.currentUser?.uid.orEmpty())
        .catch {
            android.util.Log.w("CallsVM", "call history flow failed", it)
            emit(emptyList())
        }
        .onEach { entries -> fetchMissingAvatars(entries) }

    private val aliases = userRepository.observeContactAliases()
        .catch { emit(emptyMap()) }
        .onStart { emit(emptyMap()) }

    val calls: StateFlow<List<CallHistoryEntry>> =
        combine(history, avatars, aliases) { entries, avatarMap, aliasMap ->
            entries.map { entry ->
                entry.copy(
                    partnerName = resolveDisplayName(entry.partnerName, aliasMap[entry.partnerId]),
                    partnerAvatarUrl = avatarMap[entry.partnerId] ?: entry.partnerAvatarUrl,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun fetchMissingAvatars(entries: List<CallHistoryEntry>) {
        val ids = entries
            .map { it.partnerId }
            .distinct()
            .filter { it.isNotBlank() && fetchedIds.add(it) }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { id ->
                val url = runCatching {
                    userRepository.getUserById(id).getOrNull()?.avatarUrl
                }.getOrNull()
                if (!url.isNullOrBlank()) {
                    avatars.update { it + (id to url) }
                }
            }
        }
    }
}
