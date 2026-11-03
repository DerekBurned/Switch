package com.example.messenger.presentation.screens.chatuserprofile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.messenger.data.media.ClipPlaybackController
import com.example.messenger.data.media.ClipPlaybackState
import com.example.messenger.data.media.ClipQueueItem
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.repository.IClipRepository
import com.example.messenger.domain.repository.IConversationRepository
import com.example.messenger.domain.repository.IMediaRepository
import com.example.messenger.domain.repository.IMessageRepository
import com.example.messenger.domain.repository.IUserRepository
import com.example.messenger.domain.usecase.user.GetUserByIdUseCase
import com.example.messenger.presentation.base.toUiText
import com.example.messenger.util.Resource
import com.example.messenger.util.resolveDisplayName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatUserProfileViewModel @Inject constructor(
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val userRepository: IUserRepository,
    private val conversationRepository: IConversationRepository,
    private val messageRepository: IMessageRepository,
    private val mediaRepository: IMediaRepository,
    private val clipRepository: IClipRepository,
    private val playbackController: ClipPlaybackController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUserProfileUiState())
    val uiState: StateFlow<ChatUserProfileUiState> = _uiState.asStateFlow()

    val clipPlayback: StateFlow<ClipPlaybackState> = playbackController.state

    private val remoteMedia = MutableStateFlow<Map<String, Message>>(emptyMap())
    private var conversationId: String? = null
    private var oldestRemoteId: String? = null
    private var pageJob: Job? = null

    init {
        val userId: String? = savedStateHandle["userId"]
        val seededAvatarUrl: String? = savedStateHandle.get<String>("avatarUrl")?.takeIf { it.isNotBlank() }
        val seededName: String? = savedStateHandle.get<String>("partnerName")?.takeIf { it.isNotBlank() }
        val navConversationId: String? = savedStateHandle.get<String>("conversationId")?.takeIf { it.isNotBlank() }
        if (seededAvatarUrl != null || seededName != null) {
            _uiState.update {
                it.copy(
                    seededAvatarUrl = seededAvatarUrl ?: it.seededAvatarUrl,
                    displayName = seededName ?: it.displayName,
                )
            }
        }
        if (!userId.isNullOrBlank()) {
            _uiState.update { it.copy(partnerId = userId) }
            load(userId)
            loadPhotos(userId)
            loadSharedMedia(userId, navConversationId)
        }
    }

    private fun loadPhotos(userId: String) {
        viewModelScope.launch {
            userRepository.getProfilePhotos(userId).onSuccess { urls ->
                _uiState.update { it.copy(avatarPhotos = urls) }
            }
        }
    }

    private fun load(userId: String) {
        viewModelScope.launch {
            combine(
                getUserByIdUseCase(userId),
                userRepository.observeContactAliases(),
            ) { resource, aliases -> resource to aliases[userId] }
                .collect { (resource, alias) ->
                    when (resource) {
                        is Resource.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                        is Resource.Success -> _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = resource.data,
                                displayName = resolveDisplayName(
                                    rawName = resource.data?.username,
                                    alias = alias,
                                    deleted = resource.data?.deleted == true,
                                ),
                                isOnline = resource.data?.isOnline == true && resource.data?.deleted != true,
                            )
                        }
                        is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = resource.message.toUiText()) }
                        is Resource.Failure -> _uiState.update { it.copy(isLoading = false, error = resource.exception.message?.toUiText()) }
                    }
                }
        }
    }

    private fun loadSharedMedia(partnerId: String, navConversationId: String?) {
        _uiState.update { it.copy(mediaLoading = true) }
        viewModelScope.launch {
            val resolved = navConversationId ?: findConversationWith(partnerId)
            if (resolved == null) {
                _uiState.update { it.copy(mediaLoading = false, mediaEndReached = true) }
                return@launch
            }
            conversationId = resolved
            mediaRepository.transfers
                .onEach { transfers -> _uiState.update { it.copy(transfers = transfers) } }
                .launchIn(viewModelScope)
            combine(messageRepository.getMessagesStream(resolved), remoteMedia) { local, remote ->
                sharedMediaOf((remote + local.associateBy { it.id }).values)
            }
                .onEach { media -> _uiState.update { it.copy(sharedMedia = media) } }
                .launchIn(viewModelScope)
            _uiState.update { it.copy(mediaLoading = false) }
            loadMoreMedia()
        }
    }

    private suspend fun findConversationWith(partnerId: String): String? =
        conversationRepository.getAllConversations().first()
            .firstOrNull { it.participantIds.size == 2 && partnerId in it.participantIds }
            ?.id

    fun onMediaTabChange(tab: MediaTab) = _uiState.update { it.copy(mediaTab = tab) }

    fun loadMoreMedia() {
        val conversation = conversationId ?: return
        if (pageJob?.isActive == true || _uiState.value.mediaEndReached) return
        _uiState.update { it.copy(mediaLoading = true) }
        pageJob = viewModelScope.launch {
            messageRepository.fetchSharedMediaPage(conversation, oldestRemoteId, MEDIA_PAGE_SIZE)
                .onSuccess { page ->
                    page.lastOrNull()?.let { oldestRemoteId = it.id }
                    remoteMedia.update { current -> current + page.associateBy { it.id } }
                    _uiState.update {
                        it.copy(
                            mediaLoading = false,
                            mediaEndReached = page.size < MEDIA_PAGE_SIZE,
                            mediaPagesLoaded = it.mediaPagesLoaded + 1,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(mediaLoading = false, mediaEndReached = true) }
                }
        }
    }

    fun toggleVoice(messageId: String) {
        val conversation = conversationId ?: return
        val voice = _uiState.value.sharedMedia.voice
        val message = voice.firstOrNull { it.id == messageId } ?: return
        voice.forEach(clipRepository::registerRemoteClip)
        playbackController.toggle(ClipQueueItem(message.id, conversation, message.clip.durationMs))
    }

    fun downloadMedia(item: MediaItem) = mediaRepository.downloadMedia(item)

    fun cancelDownload(itemId: String) = mediaRepository.cancelDownload(itemId)

    fun cancelUpload(messageId: String, itemId: String) = mediaRepository.cancelUpload(messageId, itemId)

    companion object {
        const val MEDIA_PAGE_SIZE = 60L
    }
}
