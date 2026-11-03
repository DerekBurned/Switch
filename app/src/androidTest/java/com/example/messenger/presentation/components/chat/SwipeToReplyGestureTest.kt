package com.example.messenger.presentation.components.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SwipeToReplyGestureTest {

    @get:Rule
    val compose = createComposeRule()

    private var replies = 0

    private fun swipeRight(distance: Dp) {
        compose.setContent {
            SwipeToReply(onReply = { replies++ }) {
                Box(modifier = Modifier.size(width = 300.dp, height = 64.dp).testTag(TAG))
            }
        }
        compose.onNodeWithTag(TAG).performTouchInput {
            val start = Offset(16.dp.toPx(), centerY)
            val totalPx = distance.toPx()
            down(start)
            for (step in 1..STEPS) {
                moveTo(start + Offset(totalPx * step / STEPS, 0f))
            }
            up()
        }
        compose.waitForIdle()
    }

    @Test
    fun hundredDpSwipeReplies() {
        swipeRight(100.dp)
        assertEquals(1, replies)
    }

    @Test
    fun fiftyDpSwipeDoesNotReply() {
        swipeRight(50.dp)
        assertEquals(0, replies)
    }

    private companion object {
        const val TAG = "swipe_target"
        const val STEPS = 20
    }
}
