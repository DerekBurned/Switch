package com.example.messenger.presentation.screens.call

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.messenger.domain.service.ICallService
import com.example.messenger.presentation.components.call.CallVideoView
import com.example.messenger.presentation.components.call.VideoSurfaceOwner
import com.example.messenger.presentation.components.call.model.SurfaceKind
import com.example.messenger.presentation.components.call.model.VideoStream
import kotlin.math.roundToInt

@Composable
internal fun DraggableLocalPreview(
    parentSize: IntSize,
    callService: ICallService,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val tileWidth = 110.dp
    val tileHeight = 160.dp
    val margin = 16.dp
    var offset by remember { mutableStateOf(Offset.Zero) }

    val tileWidthPx = with(density) { tileWidth.toPx() }
    val tileHeightPx = with(density) { tileHeight.toPx() }
    val marginPx = with(density) { margin.toPx() }
    val minX = -(parentSize.width - tileWidthPx - 2 * marginPx).coerceAtLeast(0f)
    val maxY = (parentSize.height - tileHeightPx - 2 * marginPx).coerceAtLeast(0f)

    CallVideoView(
        host = VideoSurfaceOwner.Host.FULLSCREEN,
        stream = VideoStream.Local,
        service = callService,
        kind = SurfaceKind.TEXTURE,
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 72.dp)
            .padding(margin)
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .size(tileWidth, tileHeight)
            .clip(RoundedCornerShape(14.dp))
            .pointerInput(minX, maxY) {
                detectDragGestures { change, drag ->
                    change.consume()
                    offset = Offset(
                        (offset.x + drag.x).coerceIn(minX, 0f),
                        (offset.y + drag.y).coerceIn(0f, maxY),
                    )
                }
            },
    )
}
