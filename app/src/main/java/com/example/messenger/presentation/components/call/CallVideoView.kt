package com.example.messenger.presentation.components.call

import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.messenger.domain.service.ICallService
import com.example.messenger.presentation.components.call.model.SurfaceKind
import com.example.messenger.presentation.components.call.model.VideoStream

@Composable
fun CallVideoView(
    host: VideoSurfaceOwner.Host,
    stream: VideoStream,
    service: ICallService,
    modifier: Modifier = Modifier,
    kind: SurfaceKind = SurfaceKind.TEXTURE,
    zOrderMediaOverlay: Boolean = false,
) {
    var view by remember { mutableStateOf<View?>(null) }

    DisposableEffect(host, stream) {
        VideoSurfaceOwner.mount(host, stream)
        onDispose { VideoSurfaceOwner.unmount(host, stream) }
    }

    val owners by VideoSurfaceOwner.owners.collectAsState()
    val isOwner = owners[stream] == host

    DisposableEffect(isOwner, view, stream) {
        val target = view
        if (!isOwner || target == null) return@DisposableEffect onDispose { }

        when (stream) {
            VideoStream.Local -> service.bindLocalVideo(target)
            is VideoStream.Remote -> service.bindRemoteVideo(target, stream.uid)
        }

        onDispose {
            if (VideoSurfaceOwner.ownerOf(stream) == host) {
                when (stream) {
                    VideoStream.Local -> service.unbindLocalVideo()
                    is VideoStream.Remote -> service.unbindRemoteVideo(stream.uid)
                }
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            when (kind) {
                SurfaceKind.TEXTURE -> TextureView(ctx)
                SurfaceKind.SURFACE -> SurfaceView(ctx).apply {
                    if (zOrderMediaOverlay) setZOrderMediaOverlay(true)
                }
            }.also { view = it }
        },
        onRelease = { view = null },
        modifier = modifier,
    )
}
