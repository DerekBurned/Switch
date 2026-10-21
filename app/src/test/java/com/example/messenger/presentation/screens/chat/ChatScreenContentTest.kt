package com.example.messenger.presentation.screens.chat

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.example.messenger.data.media.ClipPlaybackState
import com.example.messenger.domain.model.Message
import com.example.messenger.presentation.theme.MessengerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatScreenContentTest {

    @get:Rule
    val compose = createComposeRule()

    private var sendClicks = 0
    private var clearReplyClicks = 0

    private fun render(state: ChatUiState, messageText: String = "") {
        compose.setContent {
            MessengerTheme {
                ChatScreenContent(
                    uiState = state,
                    messageText = messageText,
                    onMessageTextChange = {},
                    onSendClick = { sendClicks++ },
                    onBackClick = {},
                    onCallClick = {},
                    onVideoCallClick = {},
                    onIntercultorProfileClick = { _, _ -> },
                    onCopy = {},
                    onReply = {},
                    onReplyClick = {},
                    onDelete = { _, _ -> },
                    onClearReply = { clearReplyClicks++ },
                    onAttachmentClick = {},
                    onClearAttachments = {},
                    onMessagesSeen = {},
                    onLoadOlderMessages = {},
                    onDownload = {},
                    onCancelUpload = { _, _ -> },
                    onCancelDownload = {},
                    onRetryMedia = {},
                    onPlayClip = {},
                    onSeekClip = {},
                    clipPlayback = mutableStateOf(ClipPlaybackState()),
                    onRetryClip = {},
                    onCancelClipSend = {},
                    onStartRecording = {},
                    onLockRecording = {},
                    onCancelRecording = {},
                    onFinishRecording = {},
                    onInterruptRecording = {},
                    onSendVoiceDraft = {},
                    onDiscardVoiceDraft = {},
                )
            }
        }
    }

    private fun text(id: String, body: String) =
        Message.Text(id = id, conversationId = "c1", senderId = "bob", timestamp = 1, text = body)

    @Test
    fun `text messages are shown`() {
        render(ChatUiState(currentUserId = "me", messages = listOf(text("m1", "hello there"))))

        compose.onNodeWithText("hello there").assertExists()
    }

    @Test
    fun `with text typed, the send action calls onSendClick`() {
        render(ChatUiState(currentUserId = "me"), messageText = "hi")

        compose.onNodeWithContentDescription("Send").performClick()

        assertThat(sendClicks).isEqualTo(1)
    }

    @Test
    fun `the reply banner shows the replied text`() {
        render(ChatUiState(currentUserId = "me", replyingTo = text("r1", "original words")))

        compose.onNodeWithText("original words").assertExists()
    }

    @Test
    fun `closing the reply banner calls onClearReply`() {
        render(ChatUiState(currentUserId = "me", replyingTo = text("r1", "original words")))

        compose.onNodeWithContentDescription("Cancel reply").performClick()

        assertThat(clearReplyClicks).isEqualTo(1)
    }
}
