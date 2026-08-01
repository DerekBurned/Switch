package com.example.messenger.presentation.components.media

import com.example.messenger.presentation.components.common.MessageStatusIcon
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.messenger.data.media.BlurHashCache
import com.example.messenger.data.media.MediaCache
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.MediaTransfer
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus

import com.example.messenger.presentation.screens.ui.theme.BubbleSent
import com.example.messenger.presentation.screens.ui.theme.BubbleSentTop
import com.example.messenger.presentation.screens.ui.theme.LightGray
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import com.example.messenger.util.DateUtils
import java.io.File

private val ALBUM_WIDTH = 240.dp

@Composable
fun MediaAlbumGrid(
    message: Message.Media,
    isMe: Boolean,
    transfers: Map<String, MediaTransfer>,
    onOpen: (MediaItem) -> Unit,
    onDownload: (MediaItem) -> Unit,
    onCancelUpload: (messageId: String, itemId: String) -> Unit,
    onCancelDownload: (itemId: String) -> Unit,
) {
    val tokens = messengerTokens
    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 5.dp, bottomStart = 18.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 5.dp)
    }
    val containerModifier = if (isMe) {
        Modifier
            .width(ALBUM_WIDTH)
            .clip(bubbleShape)
            .background(brush = Brush.linearGradient(listOf(BubbleSentTop, BubbleSent)), shape = bubbleShape)
            .padding(3.dp)
    } else {
        Modifier
            .width(ALBUM_WIDTH)
            .clip(bubbleShape)
            .background(color = tokens.cardFill, shape = bubbleShape)
            .border(1.dp, tokens.panelBorder, bubbleShape)
            .padding(3.dp)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
    ) {
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            Column(
                modifier = containerModifier,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                AlbumLayout(
                    message = message,
                    transfers = transfers,
                    onOpen = onOpen,
                    onDownload = onDownload,
                    onCancelUpload = onCancelUpload,
                    onCancelDownload = onCancelDownload,
                )
                if (message.caption.isNotBlank()) {
                    Text(
                        text = message.caption,
                        color = if (isMe) Color.White else tokens.textPrimary,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
            ) {
                Text(
                    text = DateUtils.formatMessageTime(message.timestamp),
                    color = tokens.textMuted,
                    fontSize = 11.sp,
                )
                if (isMe) MessageStatusIcon(status = message.status)
            }
        }
    }
}

@Composable
private fun AlbumLayout(
    message: Message.Media,
    transfers: Map<String, MediaTransfer>,
    onOpen: (MediaItem) -> Unit,
    onDownload: (MediaItem) -> Unit,
    onCancelUpload: (messageId: String, itemId: String) -> Unit,
    onCancelDownload: (itemId: String) -> Unit,
) {
    val items = message.items
    val cell: @Composable (MediaItem, Modifier) -> Unit = { item, modifier ->
        MediaCell(
            item = item,
            transfer = transfers[item.id],
            isSent = message.status == MessageStatus.SENT,
            modifier = modifier,
            onOpen = { onOpen(item) },
            onDownload = { onDownload(item) },
            onCancelUpload = { onCancelUpload(message.id, item.id) },
            onCancelDownload = { onCancelDownload(item.id) },
        )
    }

    when (items.size) {
        0 -> Unit
        1 -> {
            val item = items.first()
            val ratio = if (item.width > 0 && item.height > 0) {
                item.width.toFloat() / item.height
            } else {
                1f
            }
            cell(item, Modifier.fillMaxWidth().aspectRatio(ratio.coerceIn(0.6f, 1.7f)).clip(RoundedCornerShape(10.dp)))
        }
        2 -> Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            cell(items[0], Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp)))
            cell(items[1], Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp)))
        }
        3 -> Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            cell(items[0], Modifier.weight(1.7f).aspectRatio(0.85f).clip(RoundedCornerShape(8.dp)))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                cell(items[1], Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(8.dp)))
                cell(items[2], Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(8.dp)))
            }
        }
        4 -> MediaGrid(items, columns = 2, cell = cell)
        else -> MediaGrid(items, columns = 3, cell = cell)
    }
}

@Composable
private fun MediaGrid(
    items: List<MediaItem>,
    columns: Int,
    cell: @Composable (MediaItem, Modifier) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        items.chunked(columns).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                rowItems.forEach { item ->
                    cell(item, Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(6.dp)))
                }
                repeat(columns - rowItems.size) {
                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
private fun MediaCell(
    item: MediaItem,
    transfer: MediaTransfer?,
    isSent: Boolean,
    modifier: Modifier,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onCancelUpload: () -> Unit,
    onCancelDownload: () -> Unit,
) {
    val context = LocalContext.current
    val file = remember(item.id, item.kind) { mediaCacheFile(context, item) }
    val downloaded = remember(item.id, transfer) { file.exists() && file.length() > 0 }
    var blur by remember(item.blurHash) { mutableStateOf(BlurHashCache.peek(item.blurHash)) }
    LaunchedEffect(item.blurHash) {
        if (blur == null) blur = BlurHashCache.get(item.blurHash)
    }

    Box(
        modifier = modifier
            .background(LightGray)
            .clickable(enabled = downloaded && transfer == null) { onOpen() },
        contentAlignment = Alignment.Center,
    ) {
        when (transfer) {
            is MediaTransfer.Uploading -> {
                MediaContent(item, file, blur, showFile = true, showPlay = false)
                TransferScrim(progress = transfer.progress, onCancel = onCancelUpload)
            }
            is MediaTransfer.Downloading -> {
                MediaContent(item, file, blur, showFile = false, showPlay = false)
                TransferScrim(progress = transfer.progress, onCancel = onCancelDownload)
            }
            else -> {
                if (downloaded) {
                    MediaContent(item, file, blur, showFile = true, showPlay = item.kind == MediaItem.VIDEO)
                } else {
                    MediaContent(item, file, blur, showFile = false, showPlay = false)
                    DownloadButton(onDownload)
                }
            }
        }
    }
}

@Composable
private fun MediaContent(
    item: MediaItem,
    file: File,
    blur: androidx.compose.ui.graphics.ImageBitmap?,
    showFile: Boolean,
    showPlay: Boolean,
) {
    if (showFile) {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    } else if (blur != null) {
        Image(
            bitmap = blur,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
    if (showPlay) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun TransferScrim(progress: Float, onCancel: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.size(46.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
                strokeWidth = 3.dp,
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun DownloadButton(onDownload: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDownload),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "Download",
            tint = Color.White,
            modifier = Modifier.size(26.dp),
        )
    }
}

fun mediaCacheFile(context: Context, item: MediaItem): File =
    File(File(context.cacheDir, MediaCache.DIR), MediaCache.fileName(item.id, item.kind))
