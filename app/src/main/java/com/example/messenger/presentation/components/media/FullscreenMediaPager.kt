package com.example.messenger.presentation.components.media

import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.example.messenger.R
import com.example.messenger.data.media.BlurHash
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class MediaSource(
    val key: String,
    val isVideo: Boolean,
    val model: Any,
    val playUri: Uri,
    val blurHash: String? = null,
)

@Composable
fun FullscreenMediaPager(
    sources: List<MediaSource>,
    startIndex: Int,
    onDismiss: () -> Unit,
    onPageSettled: (Int) -> Unit = {},
    onAddClick: (() -> Unit)? = null,
) {
    if (sources.isEmpty()) return
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        configuration.screenHeightDp.dp.toPx()
    }
    val dismissThreshold = screenHeightPx * 0.25f

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, sources.lastIndex),
        pageCount = { sources.size },
    )
    val scope = rememberCoroutineScope()
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val springBack = remember { Animatable(0f) }
    var springBackJob by remember { mutableStateOf<Job?>(null) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = true }
    }
    val playerView = remember {
        (LayoutInflater.from(context)
            .inflate(R.layout.fullscreen_player_view, null) as PlayerView).apply {
            useController = true
            controllerAutoShow = false
            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "release player")
            playerView.player = null
            exoPlayer.release()
        }
    }

    LaunchedEffect(sources) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                onPageSettled(page)
                val settled = sources.getOrNull(page)
                if (settled != null && settled.isVideo) {
                    Log.d(TAG, "bind video page=$page")
                    playerView.player = exoPlayer
                    exoPlayer.setMediaItem(Media3Item.fromUri(settled.playUri))
                    exoPlayer.prepare()
                    exoPlayer.play()
                } else {
                    exoPlayer.pause()
                    exoPlayer.clearMediaItems()
                }
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ApplyBlurBehind()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val dragProgress = (abs(dragOffsetY) / screenHeightPx).coerceIn(0f, 1f)
                    drawRect(color = Color.Black, alpha = (1f - dragProgress) * 0.92f)
                },
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val source = sources[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (page == pagerState.currentPage) {
                                val dragProgress =
                                    (abs(dragOffsetY) / screenHeightPx).coerceIn(0f, 1f)
                                translationY = dragOffsetY
                                val scale = (1f - dragProgress * 0.3f).coerceIn(0.7f, 1f)
                                scaleX = scale
                                scaleY = scale
                            }
                        }
                        .pointerInput(page) {
                            detectVerticalDragGestures(
                                onDragStart = { springBackJob?.cancel() },
                                onDragEnd = {
                                    if (abs(dragOffsetY) > dismissThreshold) {
                                        Log.d(TAG, "dismiss via drag")
                                        onDismiss()
                                    } else {
                                        springBackJob = scope.launch {
                                            springBack.snapTo(dragOffsetY)
                                            springBack.animateTo(0f) { dragOffsetY = value }
                                        }
                                    }
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    dragOffsetY += dragAmount
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    val placeholder = remember(source.blurHash) {
                        source.blurHash?.let { BlurHash.decode(it, 32, 32) }
                            ?.let { BitmapPainter(it.asImageBitmap()) }
                    }
                    AsyncImage(
                        model = source.model,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        placeholder = placeholder,
                        error = placeholder,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (source.isVideo && page == pagerState.settledPage) {
                        AndroidView(
                            factory = {
                                (playerView.parent as? ViewGroup)?.removeView(playerView)
                                playerView
                            },
                            update = { it.player = exoPlayer },
                            onRelease = {
                                if (pagerState.settledPage == page) it.player = null
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            if (onAddClick != null) {
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add photo", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ApplyBlurBehind() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.parent as? DialogWindowProvider)?.window
        if (window != null) {
            window.setBackgroundDrawable(ColorDrawable(0x00000000))
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.attributes = window.attributes.apply { dimAmount = 0.55f }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply { blurBehindRadius = 48 }
            }
        }
        onDispose { }
    }
}

private const val TAG = "FullscreenMediaPager"
