package com.example.messenger.presentation.components.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordGestureTest {

    @get:Rule
    val compose = createComposeRule()

    private var taps = 0
    private var starts = 0
    private var locks = 0
    private var cancels = 0
    private var finishes = 0
    private var interrupts = 0

    private fun showMic() {
        compose.setContent {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .testTag(MIC)
                    .recordGesture(
                        enabled = true,
                        onTap = { taps++ },
                        onStart = { starts++ },
                        onLock = { locks++ },
                        onCancel = { cancels++ },
                        onFinish = { finishes++ },
                        onInterrupt = { interrupts++ },
                        onDrag = {},
                    ),
            )
        }
    }

    @Test
    fun aQuickTapSwitchesModeAndNeverStartsRecording() {
        showMic()
        compose.onNodeWithTag(MIC).performTouchInput {
            down(center)
            advanceEventTime(80)
            up()
        }
        compose.waitForIdle()
        assertEquals(1, taps)
        assertEquals(0, starts)
        assertEquals(0, finishes)
    }

    @Test
    fun holdingStartsRecordingAndReleasingSends() {
        showMic()
        compose.onNodeWithTag(MIC).performTouchInput {
            down(center)
            advanceEventTime(400)
            up()
        }
        compose.waitForIdle()
        assertEquals(0, taps)
        assertEquals(1, starts)
        assertEquals(1, finishes)
        assertEquals(0, interrupts)
    }

    @Test
    fun slidingUpLocksAndReleasingDoesNotSend() {
        showMic()
        compose.onNodeWithTag(MIC).performTouchInput {
            down(center)
            advanceEventTime(300)
            moveBy(Offset(0f, -(LOCK_THRESHOLD.toPx() + 20f)))
            advanceEventTime(50)
            up()
        }
        compose.waitForIdle()
        assertEquals(1, starts)
        assertEquals(1, locks)
        assertEquals(0, finishes)
        assertEquals(0, cancels)
    }

    @Test
    fun slidingLeftCancelsAndReleasingDoesNotSend() {
        showMic()
        compose.onNodeWithTag(MIC).performTouchInput {
            down(center)
            advanceEventTime(300)
            moveBy(Offset(-(CANCEL_THRESHOLD.toPx() + 20f), 0f))
            advanceEventTime(50)
            up()
        }
        compose.waitForIdle()
        assertEquals(1, starts)
        assertEquals(1, cancels)
        assertEquals(0, finishes)
        assertEquals(0, locks)
    }

    private companion object {
        const val MIC = "mic"
    }
}
