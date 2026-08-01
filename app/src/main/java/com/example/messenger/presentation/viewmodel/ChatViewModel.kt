package com.example.messenger.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.data.repository.RecentContactsRepository
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.repository.IMediaRepository
import com.example.messenger.presentation.state.PendingAttachment
import com.example.messenger.domain.usecase.message.DeleteMessageUseCase
import com.example.messenger.domain.usecase.message.GetMessagesUseCase
import com.example.messenger.domain.usecase.message.LoadOlderMessagesUseCase
import com.example.messenger.domain.usecase.conversation.MarkConversationAsReadUseCase
import com.example.messenger.domain.usecase.message.MarkMessageAsReadUseCase
import com.example.messenger.domain.usecase.message.SendMessageUseCase
import com.example.messenger.domain.usecase.message.SyncMessagesUseCase
import com.example.messenger.domain.usecase.presence.ObserveUserPresenceUseCase
import com.example.messenger.domain.usecase.typing.ObserveTypingUseCase
import com.example.messenger.domain.service.ITypingService
import com.example.messenger.R
import com.example.messenger.presentation.base.MviViewModel
import com.example.messenger.presentation.base.UiText
import com.example.messenger.presentation.base.toUiText
import com.example.messenger.presentation.effect.ChatEffect
import com.example.messenger.presentation.intent.ChatIntent
import com.example.messenger.presentation.state.ChatUiState
import com.example.messenger.util.DateUtils
import com.example.messenger.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val syncMessagesUseCase: SyncMessagesUseCase,
    private val loadOlderMessagesUseCase: LoadOlderMessagesUseCase,
    private val markMessageAsReadUseCase: MarkMessageAsReadUseCase,
    private val markConversationAsReadUseCase: MarkConversationAsReadUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase,
    private val observeUserPresenceUseCase: ObserveUserPresenceUseCase,
    private val observeTypingUseCase: ObserveTypingUseCase,
    private val typingService: ITypingService,
    private val mediaRepository: IMediaRepository,
    private val userRepository: com.example.messenger.domain.repository.IUserRepository,
    private val recentContactsRepository: RecentContactsRepository,
    firebaseAuthService: FirebaseAuthService,
    savedStateHandle: SavedStateHandle,
) : MviViewModel<ChatUiState, ChatIntent, ChatEffect>(initialState = ChatUiState()) {

    val conversationId: String = savedStateHandle.get<String>("conversationId") ?: ""
    private val partnerId: String = savedStateHandle.get<String>("partnerId") ?: ""
    private val partnerName: String = savedStateHandle.get<String>("partnerName") ?: ""
    private val seededAvatarUrl: String? =
        savedStateHandle.get<String>("partnerAvatarUrl")?.takeIf { it.isNotBlank() }

    private var typingHeartbeatJob: Job? = null
    private var typingStopJob: Job? = null
    private var isTyping = false
    private var highlightClearJob: Job? = null
    private var openedAvatarRecorded: Boolean = false

    private val markedAsReadIds = mutableSetOf<String>()

    private companion object {
        const val TAG = "ChatViewModel"
        const val PAGE_SIZE = 100L
        const val TYPING_HEARTBEAT_MS = 3000L
        const val TYPING_STOP_DELAY_MS = 2000L
        const val LAST_SEEN_TICK_MS = 60_000L
        const val HIGHLIGHT_MS = 2500L

        const val MAX_JUMP_PAGES = 50
        const val JUMP_PAGE_TIMEOUT_MS = 2000L
    }

    init {
        val currentUserId = firebaseAuthService.getCurrentUserId().orEmpty()
        setState {
            copy(
                currentUserId = currentUserId,
                partnerUsername = com.example.messenger.util.resolveDisplayName(partnerName, null),
                partnerAvatarUrl = seededAvatarUrl,
                partnerLastSeenDisplay = DateUtils.formatLastSeen(partnerPresence.lastSeen),
            )
        }
        observePartnerAlias()
        observePartnerAvatar()
        observeTransfers()
        if (partnerId.isNotBlank()) {
            viewModelScope.launch {
                recentContactsRepository.recordOpened(partnerId, partnerName, seededAvatarUrl)
            }
        }
        if (conversationId.isNotBlank()) {
            loadMessages()
            syncMessages()
            markConversationRead()
            if (partnerId.isNotBlank()) {
                observePartnerPresence()
            }
            observeTyping()
        }
    }

    private fun markConversationRead() {
        viewModelScope.launch {
            markConversationAsReadUseCase(conversationId)
        }
    }

    private fun observePartnerAlias() {
        if (partnerId.isBlank()) return
        userRepository.observeContactAliases()
            .onEach { aliases ->
                setState {
                    copy(partnerUsername = com.example.messenger.util.resolveDisplayName(partnerName, aliases[partnerId]))
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observePartnerAvatar() {
        if (partnerId.isBlank()) return
        userRepository.observeUser(partnerId)
            .onEach { user ->
                val avatar = user?.avatarUrl?.takeIf { it.isNotBlank() }
                setState { copy(partnerAvatarUrl = avatar ?: partnerAvatarUrl) }
                if (avatar != null && !openedAvatarRecorded) {
                    openedAvatarRecorded = true
                    recentContactsRepository.recordOpened(partnerId, partnerName, avatar)
                }
            }
            .launchIn(viewModelScope)
        viewModelScope.launch { userRepository.getUserById(partnerId) }
    }

    override fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.TextChanged -> onTextChanged(intent.text)
            is ChatIntent.SendMessage -> sendMessage(intent.text)
            is ChatIntent.MessagesSeen -> onMessagesSeen(intent.messages)
            ChatIntent.LoadOlderMessages -> loadOlder()
            is ChatIntent.DeleteMessage -> deleteMessage(intent.message, intent.forEveryone)
            is ChatIntent.SetReplyTo -> setState { copy(replyingTo = intent.message) }
            ChatIntent.ClearReply -> setState { copy(replyingTo = null) }
            is ChatIntent.JumpToMessage -> jumpToMessage(intent.messageId)
            is ChatIntent.Forward -> setState { copy(forwardingMessage = intent.message) }
            ChatIntent.ClearForward -> setState { copy(forwardingMessage = null) }
            ChatIntent.ClearError -> setState { copy(error = null) }
            is ChatIntent.DownloadMedia -> mediaRepository.downloadMedia(intent.item)
            is ChatIntent.CancelUpload -> mediaRepository.cancelUpload(intent.messageId, intent.itemId)
            is ChatIntent.CancelDownload -> mediaRepository.cancelDownload(intent.itemId)
            is ChatIntent.ToggleAttachment -> toggleAttachment(intent.uri, intent.kind)
            is ChatIntent.AddAttachment -> addAttachment(intent.uri, intent.kind)
            ChatIntent.ClearAttachments -> setState { copy(pendingAttachments = emptyList()) }
            is ChatIntent.RetryMedia -> mediaRepository.retry(intent.messageId)
        }
    }

    private fun loadOlder() {
        if (currentState.isLoadingOlder || !currentState.hasMoreOlder) return
        viewModelScope.launch {
            setState { copy(isLoadingOlder = true) }
            loadOlderPage()
            setState { copy(isLoadingOlder = false) }
        }
    }

    private suspend fun loadOlderPage(): Int {
        val result = loadOlderMessagesUseCase(conversationId)
        val fetched = result.getOrDefault(0)
        if (result.isFailure) {
            Log.w(TAG, "Loading older messages failed", result.exceptionOrNull())
        }
        setState {
            copy(hasMoreOlder = if (result.isSuccess) fetched >= PAGE_SIZE else hasMoreOlder)
        }
        return fetched
    }

    private fun jumpToMessage(messageId: String) {
        viewModelScope.launch {
            var guard = 0
            while (currentState.messages.none { it.id == messageId } &&
                currentState.hasMoreOlder &&
                guard < MAX_JUMP_PAGES
            ) {
                guard++
                val sizeBefore = currentState.messages.size
                loadOlderPage()
                
                withTimeoutOrNull(JUMP_PAGE_TIMEOUT_MS) {
                    state.first { s ->
                        s.messages.size > sizeBefore || s.messages.any { it.id == messageId }
                    }
                }
                if (currentState.messages.size == sizeBefore) break 
            }

            if (currentState.messages.any { it.id == messageId }) {
                setState { copy(highlightedMessageId = messageId) }
                highlightClearJob?.cancel()
                highlightClearJob = viewModelScope.launch {
                    delay(HIGHLIGHT_MS)
                    setState { copy(highlightedMessageId = null) }
                }
            } else {
                emitEffect(ChatEffect.ShowError("Original message not found".toUiText()))
            }
        }
    }

    private fun syncMessages() {
        viewModelScope.launch {
            try {
                syncMessagesUseCase(conversationId).collect { }
            } catch (e: Exception) {
                Log.w(TAG, "Message sync stream errored", e)
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            getMessagesUseCase(conversationId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState {

                            val resolvedAnchor = if (
                                !unreadAnchorResolved &&
                                currentUserId.isNotBlank() &&
                                resource.data.isNotEmpty()
                            ) {
                                val anchorId = resource.data.firstOrNull {
                                    it.senderId != currentUserId && it.status != MessageStatus.READ
                                }?.id
                                copy(firstUnreadMessageId = anchorId, unreadAnchorResolved = true)
                            } else {
                                this
                            }
                            val media = resource.data
                                .filterIsInstance<Message.Media>()
                                .flatMap { it.items }
                            resolvedAnchor.copy(
                                isLoading = false,
                                messages = resource.data,
                                conversationMedia = media,
                                error = null,
                            )
                        }
                    }
                    is Resource.Error -> setState { copy(isLoading = false, error = resource.message.toUiText()) }
                    is Resource.Failure -> setState { copy(isLoading = false, error = resource.exception.message?.toUiText()) }
                }
            }
        }
    }

    private fun onMessagesSeen(messages: List<Message>) {
        val me = currentState.currentUserId
        if (me.isBlank()) {
            Log.w(TAG, "Skip read receipt: currentUserId is blank (auth race?)")
            return
        }
        var newlyRead = false
        messages.forEach { message ->
            if (markedAsReadIds.add(message.id)) {
                markAsRead(message)
                newlyRead = true
            }
        }
        if (newlyRead) {
            markConversationRead()
        }
    }

    private fun observePartnerPresence() {
        viewModelScope.launch {
            val presence = observeUserPresenceUseCase(partnerId)
                .catch { e -> Log.w(TAG, "Presence observer errored", e) }
            combine(presence, tickerFlow(LAST_SEEN_TICK_MS)) { p, _ -> p }
                .collect { p ->
                    setState {
                        copy(
                            partnerPresence = p,
                            partnerLastSeenDisplay = DateUtils.formatLastSeen(p.lastSeen),
                        )
                    }
                }
        }
    }

    private fun tickerFlow(periodMs: Long) = flow {
        emit(Unit)
        while (true) {
            delay(periodMs)
            emit(Unit)
        }
    }

    private fun observeTransfers() {
        mediaRepository.transfers
            .onEach { setState { copy(transfers = it) } }
            .launchIn(viewModelScope)
    }

    private fun observeTyping() {
        viewModelScope.launch {
            observeTypingUseCase(conversationId)
                .catch { e -> Log.w(TAG, "Typing observer errored", e) }
                .collect { typingUserIds ->
                    setState {
                        copy(
                            typingUsernames = typingUserIds.toList(),
                            isPartnerTyping = typingUserIds.isNotEmpty(),
                        )
                    }
                }
        }
    }

    private fun onTextChanged(text: String) {
        if (text.isBlank()) {
            stopTyping()
            return
        }

        if (!isTyping) {
            isTyping = true
            typingHeartbeatJob = viewModelScope.launch {
                while (isActive) {
                    try { typingService.setTyping(conversationId) } catch (e: Exception) {
                        Log.w(TAG, "Typing set failed", e)
                    }
                    delay(TYPING_HEARTBEAT_MS)
                }
            }
        }

        typingStopJob?.cancel()
        typingStopJob = viewModelScope.launch {
            delay(TYPING_STOP_DELAY_MS)
            stopTyping()
        }
    }

    private fun stopTyping() {
        isTyping = false
        typingHeartbeatJob?.cancel()
        typingHeartbeatJob = null
        typingStopJob?.cancel()
        typingStopJob = null
        viewModelScope.launch {
            try { typingService.clearTyping(conversationId) } catch (e: Exception) {
                Log.w(TAG, "Typing clear failed", e)
            }
        }
    }

    private fun toggleAttachment(uri: Uri, kind: String) {
        setState {
            val existing = pendingAttachments.firstOrNull { it.uri == uri }
            val updated = if (existing != null) {
                pendingAttachments - existing
            } else {
                pendingAttachments + PendingAttachment(uri, kind)
            }
            Log.d(TAG, "toggleAttachment uri=$uri -> ${updated.size} pending")
            copy(pendingAttachments = updated)
        }
    }

    private fun addAttachment(uri: Uri, kind: String) {
        setState {
            if (pendingAttachments.any { it.uri == uri }) return@setState this
            Log.d(TAG, "addAttachment uri=$uri -> ${pendingAttachments.size + 1} pending")
            copy(pendingAttachments = pendingAttachments + PendingAttachment(uri, kind))
        }
    }

    private fun sendMessage(text: String) {
        val attachments = currentState.pendingAttachments
        if (attachments.isNotEmpty()) {
            sendMediaMessage(text, attachments)
            return
        }
        val replyTo = currentState.replyingTo
        stopTyping()
        viewModelScope.launch {
            setState { copy(isSending = true, replyingTo = null) }
            sendMessageUseCase(conversationId, text, replyTo).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {}
                    is Resource.Success -> setState { copy(isSending = false, error = null) }
                    is Resource.Error -> setState { copy(isSending = false, error = resource.message.toUiText()) }
                    is Resource.Failure -> setState { copy(isSending = false, error = resource.exception.message?.toUiText()) }
                }
            }
        }
    }

    private fun sendMediaMessage(caption: String, attachments: List<PendingAttachment>) {
        Log.d(TAG, "sendMediaMessage: ${attachments.size} attachments, captionBlank=${caption.isBlank()}")
        stopTyping()
        mediaRepository.sendMediaMessage(
            conversationId = conversationId,
            uris = attachments.map { it.uri.toString() },
            caption = caption.trim(),
        )
        setState { copy(pendingAttachments = emptyList(), replyingTo = null) }
    }

    private fun markAsRead(message: Message) {
        viewModelScope.launch {
            markMessageAsReadUseCase(message)
        }
    }

    private fun deleteMessage(message: Message, forEveryone: Boolean) {
        viewModelScope.launch {
            deleteMessageUseCase(message, forEveryone).collect { resource ->
                when (resource) {
                    is Resource.Error -> emitEffect(
                        ChatEffect.ShowError(resource.message.toUiText()),
                    )
                    is Resource.Failure -> emitEffect(
                        ChatEffect.ShowError(
                            resource.exception.message?.toUiText()
                                ?: UiText.StringResource(R.string.chat_error_delete_failed),
                        ),
                    )
                    else -> {}
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        typingHeartbeatJob?.cancel()
        typingStopJob?.cancel()
        typingService.clearTypingNow(conversationId)
    }
}
