package com.example.messenger.presentation.components.call

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.data.remote.call.ActiveCallHolder
import com.example.messenger.data.remote.call.CallUids
import com.example.messenger.domain.service.ICallService
import com.example.messenger.presentation.components.call.model.PipForm
import com.example.messenger.presentation.components.call.model.SurfaceKind
import com.example.messenger.presentation.components.call.model.VideoStream
import com.example.messenger.presentation.components.common.MessengerAvatar

@Composable
fun PipCallSurface(callService: ICallService) {
    val call by ActiveCallHolder.state.collectAsStateWithLifecycle()
    val active = call

    val remoteStream = active?.let {
        val partnerId = if (it.wasIncoming) it.callerId else it.calleeId
        if (partnerId.isBlank()) null else VideoStream.Remote(CallUids.fromUserId(partnerId))
    }

    val form = when {
        active == null || active.callEnded -> PipForm.Ended
        active.remoteVideoOn && remoteStream != null -> PipForm.Video(remoteStream)
        active.localVideoOn -> PipForm.Video(VideoStream.Local)
        else -> PipForm.Bubble
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = form, animationSpec = tween(220), label = "pipForm") { target ->
            when (target) {
                is PipForm.Video -> key(target.stream) {
                    CallVideoView(
                        host = VideoSurfaceOwner.Host.SYSTEM_PIP,
                        stream = target.stream,
                        service = callService,
                        kind = SurfaceKind.SURFACE,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                PipForm.Bubble -> PipBubble(
                    name = active?.partnerName.orEmpty(),
                    seconds = active?.seconds ?: 0,
                )

                PipForm.Ended -> PipEnded()
            }
        }
    }
}

@Composable
private fun PipBubble(name: String, seconds: Int) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(BarBackground),
        contentAlignment = Alignment.Center,
    ) {
        val diameter = min(maxWidth, maxHeight)
        Box(
            modifier = Modifier.size(diameter),
            contentAlignment = Alignment.Center,
        ) {
            MessengerAvatar(name = name, photoUrl = null, size = diameter)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(diameter * 0.045f, RingGreen, CircleShape),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = maxHeight * 0.07f)
                .clip(RoundedCornerShape(percent = 50))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = formatHms(seconds),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PipEnded() {
    Box(
        modifier = Modifier.fillMaxSize().background(BarBackground),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Call ended",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
