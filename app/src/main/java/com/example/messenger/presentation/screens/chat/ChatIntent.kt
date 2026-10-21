package com.example.messenger.presentation.screens.chat

import android.net.Uri
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.Message
import com.example.messenger.presentation.base.mvi.UiIntent

sealed interface ChatIntent : UiIntent {
    data class TextChanged(val text: String) : ChatIntent
    data class SendMessage(val text: String) : ChatIntent
    data class MessagesSeen(val messages: List<Message>) : ChatIntent
    data object LoadOlderMessages : ChatIntent
    data class DeleteMessage(val message: Message, val forEveryone: Boolean) : ChatIntent
    data class SetReplyTo(val message: Message) : ChatIntent

    data class JumpToMessage(val messageId: String) : ChatIntent
    data class Forward(val message: Message) : ChatIntent
    data object ClearReply : ChatIntent
    data object ClearForward : ChatIntent
    data object ClearError : ChatIntent
    data class DownloadMedia(val item: MediaItem) : ChatIntent
    data class CancelUpload(val messageId: String, val itemId: String) : ChatIntent
    data class CancelDownload(val itemId: String) : ChatIntent
    data class ToggleAttachment(val uri: Uri, val kind: String) : ChatIntent
    data class AddAttachment(val uri: Uri, val kind: String) : ChatIntent
    data object ClearAttachments : ChatIntent
    data class RetryMedia(val messageId: String) : ChatIntent

    data object StartRecording : ChatIntent
    data object LockRecording : ChatIntent
    data object CancelRecording : ChatIntent
    data object FinishRecording : ChatIntent
    data object InterruptRecording : ChatIntent
    data object SendVoiceDraft : ChatIntent
    data object DiscardVoiceDraft : ChatIntent
    data class MicPermissionDenied(val permanently: Boolean, val video: Boolean = false) : ChatIntent
    data object DismissMicPrompt : ChatIntent
    data object ToggleClipMode : ChatIntent

    data class PlayClip(val messageId: String) : ChatIntent
    data class SeekClip(val fraction: Float) : ChatIntent
    data class RetryClip(val messageId: String) : ChatIntent
    data class CancelClipSend(val messageId: String) : ChatIntent
}
