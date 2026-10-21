package com.example.messenger.presentation.screens.chat

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.messenger.R
import com.example.messenger.data.media.ClipPlaybackState
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.voicemessage.Clip
import com.example.messenger.presentation.components.chat.LongPressMessage
import com.example.messenger.presentation.components.chat.MessageContextMenuHost
import com.example.messenger.presentation.components.chat.SwipeToReply
import com.example.messenger.presentation.components.chat.model.MessageContextMenuState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoNoteBubbleGestureTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var toggles = 0
    private var replies = 0
    private val seeks = mutableListOf<Float>()

    private fun showNote(playing: Boolean) {
        val playback = mutableStateOf(
            ClipPlaybackState(
                messageId = MESSAGE_ID,
                conversationId = "c1",
                positionMs = 2_000L,
                durationMs = 10_000L,
                isPlaying = playing,
                isVideo = true,
            ),
        )
        compose.setContent {
            VideoNoteBubble(
                clip = Clip.Video(durationMs = 10_000L),
                messageId = MESSAGE_ID,
                isMe = false,
                played = true,
                status = MessageStatus.READ,
                timestamp = 1_700_000_000_000L,
                playback = playback,
                onTogglePlay = { toggles++ },
                onSeek = { seeks += it },
                videoFrame = { null },
                onAttachVideo = {},
                onDetachVideo = {},
            )
        }
    }

    @Test
    fun aTapInTheCentreTogglesPlayback() {
        showNote(playing = true)
        compose.onNodeWithContentDescription(context.getString(R.string.video_note_pause))
            .performTouchInput { click(center) }
        compose.waitForIdle()
        assertEquals(1, toggles)
        assertTrue(seeks.isEmpty())
    }

    @Test
    fun draggingAlongTheRingSeeksWithoutToggling() {
        showNote(playing = true)
        compose.onNodeWithContentDescription(context.getString(R.string.video_note_pause))
            .performTouchInput {
                val edge = 6.dp.toPx()
                down(Offset(width - edge, height / 2f))
                moveTo(Offset(width / 2f, height - edge))
                up()
            }
        compose.waitForIdle()
        assertEquals(0, toggles)
        assertTrue(seeks.isNotEmpty())
        assertEquals(0.25f, seeks.first(), 0.03f)
        assertEquals(0.5f, seeks.last(), 0.03f)
    }

    @Test
    fun aTapWhilePausedResumes() {
        showNote(playing = false)
        compose.onNodeWithContentDescription(context.getString(R.string.video_note_play))
            .performTouchInput { click(center) }
        compose.waitForIdle()
        assertEquals(1, toggles)
    }

    @Test
    fun aLongPressInsideTheChatWrappersOpensTheMenuWithoutToggling() {
        val menu = showWrappedNote()
        compose.onNodeWithContentDescription(context.getString(R.string.video_note_pause))
            .performTouchInput { longClick(center) }
        compose.waitForIdle()
        assertNotNull(menu.active)
        assertEquals(0, toggles)
        assertTrue(seeks.isEmpty())
    }

    @Test
    fun aSwipeOffTheRingRepliesWithoutSeekingOrToggling() {
        showWrappedNote()
        compose.onNodeWithContentDescription(context.getString(R.string.video_note_pause))
            .performTouchInput {
                val start = Offset(width * 0.3f, centerY)
                swipe(start = start, end = start + Offset(200.dp.toPx(), 0f), durationMillis = 300)
            }
        compose.waitForIdle()
        assertEquals(1, replies)
        assertEquals(0, toggles)
        assertTrue(seeks.isEmpty())
    }

    @Test
    fun seekingClockwiseFromTheTopInsideTheChatWrappersNeverReplies() {
        showWrappedNote()
        compose.onNodeWithContentDescription(context.getString(R.string.video_note_pause))
            .performTouchInput {
                val edge = 6.dp.toPx()
                down(Offset(width / 2f, edge))
                moveTo(Offset(width * 0.85f, height * 0.15f))
                moveTo(Offset(width - edge, height / 2f))
                up()
            }
        compose.waitForIdle()
        assertEquals(0, replies)
        assertEquals(0, toggles)
        assertTrue(seeks.isNotEmpty())
        assertEquals(0.25f, seeks.last(), 0.03f)
    }

    private fun showWrappedNote(): MessageContextMenuState {
        val menu = MessageContextMenuState()
        val playback = mutableStateOf(
            ClipPlaybackState(
                messageId = MESSAGE_ID,
                conversationId = "c1",
                positionMs = 2_000L,
                durationMs = 10_000L,
                isPlaying = true,
                isVideo = true,
            ),
        )
        compose.setContent {
            MessageContextMenuHost(state = menu) {
                SwipeToReply(onReply = { replies++ }) {
                    LongPressMessage(
                        isMe = false,
                        messageKey = MESSAGE_ID,
                        actions = emptyList(),
                        state = menu,
                        preview = {},
                    ) {
                        VideoNoteBubble(
                            clip = Clip.Video(durationMs = 10_000L),
                            messageId = MESSAGE_ID,
                            isMe = false,
                            played = true,
                            status = MessageStatus.READ,
                            timestamp = 1_700_000_000_000L,
                            playback = playback,
                            onTogglePlay = { toggles++ },
                            onSeek = { seeks += it },
                            videoFrame = { null },
                            onAttachVideo = {},
                            onDetachVideo = {},
                        )
                    }
                }
            }
        }
        return menu
    }

    private companion object {
        const val MESSAGE_ID = "note-1"
    }
}
