package com.example.messenger.presentation.components.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.messenger.data.media.ClipPlaybackController
import com.example.messenger.data.media.ClipPlaybackState
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.domain.repository.IConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClipChatTarget(
    val conversationId: String,
    val partnerId: String,
    val partnerName: String,
    val partnerAvatarUrl: String,
)

@HiltViewModel
class ClipPlayerViewModel @Inject constructor(
    private val controller: ClipPlaybackController,
    private val conversationRepository: IConversationRepository,
    private val authService: FirebaseAuthService,
) : ViewModel() {

    val state: StateFlow<ClipPlaybackState> = controller.state

    fun pause() = controller.pause()

    fun resume() = controller.resume()

    fun stop() = controller.stop()

    fun setSpeed(value: Float) = controller.setSpeed(value)

    fun resolveTarget(conversationId: String, onResolved: (ClipChatTarget) -> Unit) {
        viewModelScope.launch {
            val myUid = authService.getCurrentUserId().orEmpty()
            val conversation = conversationRepository.getConversationById(conversationId)
                .getOrNull()
            if (conversation == null) {
                onResolved(ClipChatTarget(conversationId, "", "", ""))
                return@launch
            }
            val index = conversation.participantIds.indexOfFirst { it != myUid }
            onResolved(
                ClipChatTarget(
                    conversationId = conversationId,
                    partnerId = conversation.participantIds.getOrNull(index).orEmpty(),
                    partnerName = conversation.participantNames.getOrNull(index).orEmpty(),
                    partnerAvatarUrl = conversation.participantAvatars.getOrNull(index).orEmpty(),
                ),
            )
        }
    }
}
