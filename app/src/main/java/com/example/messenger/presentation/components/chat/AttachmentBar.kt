package com.example.messenger.presentation.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.presentation.state.PendingAttachment
import android.net.Uri
import androidx.compose.ui.tooling.preview.Preview
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme

@Composable
fun AttachmentBar(
    attachments: List<PendingAttachment>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (attachments.isEmpty()) return
    Row(
        modifier = modifier
            .background(Color(0xFFE8EEF8))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(attachments, key = { it.uri.toString() }) { attachment ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = attachment.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(44.dp),
                    )
                    if (attachment.kind == MediaItem.VIDEO) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))
        Text(
            text = attachmentSummary(attachments),
            color = Color.DarkGray,
            fontSize = 13.sp,
        )
        IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear attachments",
                tint = Color.Gray,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun attachmentSummary(attachments: List<PendingAttachment>): String {
    val count = attachments.size
    val allPhotos = attachments.all { it.kind == MediaItem.IMAGE }
    val allVideos = attachments.all { it.kind == MediaItem.VIDEO }
    val noun = when {
        allPhotos -> if (count == 1) "photo" else "photos"
        allVideos -> if (count == 1) "video" else "videos"
        else -> "media"
    }
    return "$count $noun attached"
}

@Preview
@Composable
private fun AttachmentBarPreview() {
    MessengerTheme {
        AttachmentBar(
            attachments = listOf(
                PendingAttachment(Uri.EMPTY, MediaItem.IMAGE),
                PendingAttachment(Uri.EMPTY, MediaItem.VIDEO),
            ),
            onClear = {},
        )
    }
}
