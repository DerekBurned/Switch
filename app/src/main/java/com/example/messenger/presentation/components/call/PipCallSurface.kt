package com.example.messenger.presentation.components.call

import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.data.remote.call.ActiveCallHolder
import com.example.messenger.data.remote.call.CallUids
import com.example.messenger.domain.service.ICallService
import com.example.messenger.presentation.components.common.MessengerAvatar

@Composable
fun PipCallSurface(callService: ICallService) {
    val call by ActiveCallHolder.state.collectAsStateWithLifecycle()
    val c = call
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when {
            c == null -> Unit
            c.remoteVideoOn -> key(true) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).also { view ->
                            val partnerId = if (c.wasIncoming) c.callerId else c.calleeId
                            callService.bindRemoteVideo(view, CallUids.fromUserId(partnerId))
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            c.localVideoOn -> key(false) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).also { view -> callService.bindLocalVideo(view) }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MessengerAvatar(name = c.partnerName, photoUrl = null, size = 56.dp)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = pipTimer(c.seconds),
                    color = Color.White,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

private fun pipTimer(s: Int): String {
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
