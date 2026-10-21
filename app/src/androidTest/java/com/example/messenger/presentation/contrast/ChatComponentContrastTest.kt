package com.example.messenger.presentation.contrast

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.messenger.data.media.ClipPlaybackState
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.presence.PresenceState
import com.example.messenger.domain.model.voicemessage.Clip
import com.example.messenger.domain.model.voicemessage.ClipKind
import com.example.messenger.presentation.components.chat.ChatMessage
import com.example.messenger.presentation.components.chat.ChatTextBubble
import com.example.messenger.presentation.components.chat.ChatTopBar
import com.example.messenger.presentation.components.chat.ContextMenuContent
import com.example.messenger.presentation.components.chat.DateDivider
import com.example.messenger.presentation.components.chat.EndedCallCard
import com.example.messenger.presentation.components.chat.MissedCallCard
import com.example.messenger.presentation.components.chat.RecordingBar
import com.example.messenger.presentation.components.chat.ScrollToBottomButton
import com.example.messenger.presentation.components.chat.UnreadMessagesDivider
import com.example.messenger.presentation.components.chat.VoiceDraftBar
import com.example.messenger.presentation.components.chat.model.MessageAction
import com.example.messenger.presentation.screens.chat.VideoNoteBubble
import com.example.messenger.presentation.screens.chat.VoiceBubble
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatComponentContrastTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun messageContextMenu() = compose.assertReadableInBothThemes("ContextMenuContent") {
        ContextMenuContent(
            actions = listOf(
                MessageAction("Reply", Icons.AutoMirrored.Filled.Reply, onClick = {}),
                MessageAction("Copy", Icons.Default.ContentCopy, onClick = {}),
                MessageAction("Forward", Icons.AutoMirrored.Filled.Forward, onClick = {}),
            ),
        )
    }

    @Test
    fun chatTextBubbleFromMe() = compose.assertReadableInBothThemes("ChatTextBubble me") {
        ChatTextBubble(
            message = ChatMessage(
                text = "See you tomorrow at the usual place",
                isMe = true,
                status = MessageStatus.READ,
                timestamp = 1_700_000_000_000L,
            ),
        )
    }

    @Test
    fun chatTextBubbleFromPeer() = compose.assertReadableInBothThemes("ChatTextBubble peer") {
        ChatTextBubble(
            message = ChatMessage(
                text = "Sounds good, see you then",
                isMe = false,
                status = MessageStatus.READ,
                timestamp = 1_700_000_000_000L,
            ),
        )
    }

    @Test
    fun chatTextBubbleWithReply() = compose.assertReadableInBothThemes("ChatTextBubble reply") {
        ChatTextBubble(
            message = ChatMessage(
                text = "Agreed",
                isMe = false,
                status = MessageStatus.READ,
                timestamp = 1_700_000_000_000L,
                replyToMessageId = "m1",
                replyToText = "Shall we meet at six",
                replyToSenderLabel = "Alice",
            ),
        )
    }

    @Test
    fun chatTopBar() = compose.assertReadableInBothThemes("ChatTopBar") {
        ChatTopBar(
            partnerName = "Bob Martin",
            presenceStatusText = "online",
            statusAccent = true,
            presenceState = PresenceState.ONLINE,
            isTyping = false,
            sharedKeyPartnerId = "u2",
            onBackClick = {},
            onProfileClick = {},
        )
    }

    @Test
    fun dateDivider() = compose.assertReadableInBothThemes("DateDivider") {
        DateDivider(label = "Yesterday")
    }

    @Test
    fun unreadMessagesDivider() = compose.assertReadableInBothThemes("UnreadMessagesDivider") {
        UnreadMessagesDivider()
    }

    @Test
    fun missedCallCard() = compose.assertReadableInBothThemes("MissedCallCard") {
        MissedCallCard(onCall = {}, timestamp = 1_700_000_000_000L, isMe = false)
    }

    @Test
    fun endedCallCard() = compose.assertReadableInBothThemes("EndedCallCard") {
        EndedCallCard(durationSeconds = 125, timestamp = 1_700_000_000_000L, isMe = true)
    }

    @Test
    fun scrollToBottomButton() = compose.assertReadableInBothThemes("ScrollToBottomButton") {
        ScrollToBottomButton(unreadCount = 7, onClick = {})
    }

    @Test
    fun voiceBubbleFromPeer() = compose.assertReadableInBothThemes("VoiceBubble peer") {
        VoiceBubble(
            clip = Clip.Voice(durationMs = 12_000L, waveform = List(50) { (it * 7) % 100 }),
            messageId = "m1",
            isMe = false,
            played = false,
            status = MessageStatus.READ,
            timestamp = 1_700_000_000_000L,
            playback = remember { mutableStateOf(ClipPlaybackState()) },
            onTogglePlay = {},
            onSeek = {},
            replySenderLabel = "Alice",
            replyText = "Shall we meet at six",
        )
    }

    @Test
    fun voiceBubbleFromMe() = compose.assertReadableInBothThemes("VoiceBubble me") {
        VoiceBubble(
            clip = Clip.Voice(durationMs = 12_000L, waveform = List(50) { (it * 7) % 100 }),
            messageId = "m2",
            isMe = true,
            played = true,
            status = MessageStatus.READ,
            timestamp = 1_700_000_000_000L,
            playback = remember { mutableStateOf(ClipPlaybackState()) },
            onTogglePlay = {},
            onSeek = {},
        )
    }

    @Test
    fun recordingBar() = compose.assertReadableInBothThemes("RecordingBar", Backdrop.Card) {
        RecordingBar(
            elapsedMs = 7_300L,
            amplitudes = List(40) { (it * 7) % 100 },
            sampleCount = 40,
            locked = false,
            onCancel = {},
            onStop = {},
        )
    }

    @Test
    fun recordingBarLocked() = compose.assertReadableInBothThemes("RecordingBar locked", Backdrop.Card) {
        RecordingBar(
            elapsedMs = 7_300L,
            amplitudes = List(40) { (it * 7) % 100 },
            sampleCount = 40,
            locked = true,
            onCancel = {},
            onStop = {},
        )
    }

    @Test
    fun videoRecordingBar() = compose.assertReadableInBothThemes("RecordingBar video", Backdrop.Card) {
        RecordingBar(
            elapsedMs = 4_100L,
            amplitudes = null,
            sampleCount = 0,
            locked = false,
            onCancel = {},
            onStop = {},
        )
    }

    @Test
    fun voiceDraftBar() = compose.assertReadableInBothThemes("VoiceDraftBar", Backdrop.Card) {
        VoiceDraftBar(
            durationMs = 42_000L,
            waveform = List(50) { (it * 7) % 100 },
            onDiscard = {},
        )
    }

    @Test
    fun videoDraftBar() = compose.assertReadableInBothThemes("VoiceDraftBar video", Backdrop.Card) {
        VoiceDraftBar(
            durationMs = 9_000L,
            waveform = emptyList(),
            onDiscard = {},
            kind = ClipKind.VIDEO,
        )
    }

    @Test
    fun videoNoteFromPeer() = compose.assertReadableInBothThemes("VideoNoteBubble peer") {
        VideoNoteBubble(
            clip = Clip.Video(durationMs = 14_000L),
            messageId = "v1",
            isMe = false,
            played = false,
            status = MessageStatus.READ,
            timestamp = 1_700_000_000_000L,
            playback = remember { mutableStateOf(ClipPlaybackState()) },
            onTogglePlay = {},
            onSeek = {},
            videoFrame = { null },
            onAttachVideo = {},
            onDetachVideo = {},
        )
    }

    @Test
    fun videoNoteFromMe() = compose.assertReadableInBothThemes("VideoNoteBubble me") {
        VideoNoteBubble(
            clip = Clip.Video(durationMs = 14_000L),
            messageId = "v2",
            isMe = true,
            played = true,
            status = MessageStatus.READ,
            timestamp = 1_700_000_000_000L,
            playback = remember { mutableStateOf(ClipPlaybackState()) },
            onTogglePlay = {},
            onSeek = {},
            videoFrame = { null },
            onAttachVideo = {},
            onDetachVideo = {},
        )
    }
}
