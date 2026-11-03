package com.example.messenger.presentation.components.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.R
import com.example.messenger.data.media.ClipPlaybackState
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.MediaTransfer
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.voicemessage.Clip
import com.example.messenger.presentation.components.chat.VoiceWaveform
import com.example.messenger.presentation.components.media.MediaCell
import com.example.messenger.presentation.screens.chatuserprofile.ChatUserProfileUiState
import com.example.messenger.presentation.screens.chatuserprofile.MediaTab
import com.example.messenger.presentation.screens.chatuserprofile.SharedMediaEntry
import com.example.messenger.presentation.theme.messengerTokens
import com.example.messenger.util.DateUtils

private const val GRID_COLUMNS = 4

internal fun LazyListScope.sharedMediaSection(
    state: ChatUserProfileUiState,
    playback: State<ClipPlaybackState>,
    onOpenMedia: (SharedMediaEntry) -> Unit,
    onDownload: (MediaItem) -> Unit,
    onCancelDownload: (itemId: String) -> Unit,
    onCancelUpload: (messageId: String, itemId: String) -> Unit,
    onToggleVoice: (messageId: String) -> Unit,
    onLoadMore: () -> Unit,
) {
    val isEmpty = when (state.mediaTab) {
        MediaTab.PHOTO -> state.sharedMedia.photos.isEmpty()
        MediaTab.VIDEO -> state.sharedMedia.videos.isEmpty()
        MediaTab.VOICE -> state.sharedMedia.voice.isEmpty()
        MediaTab.FILE -> true
    }
    when (state.mediaTab) {
        MediaTab.PHOTO -> mediaGrid(
            state.sharedMedia.photos, state.transfers, onOpenMedia, onDownload, onCancelDownload, onCancelUpload,
        )
        MediaTab.VIDEO -> mediaGrid(
            state.sharedMedia.videos, state.transfers, onOpenMedia, onDownload, onCancelDownload, onCancelUpload,
        )
        MediaTab.VOICE -> items(state.sharedMedia.voice, key = { "voice-${it.id}" }) { message ->
            SharedVoiceRow(
                message = message,
                title = if (message.senderId == state.partnerId) {
                    state.displayName
                } else {
                    stringResource(R.string.profile_media_you)
                },
                playback = playback,
                onToggle = { onToggleVoice(message.id) },
            )
        }
        MediaTab.FILE -> Unit
    }
    val pages = state.mediaTab != MediaTab.FILE && !state.mediaEndReached
    when {
        state.mediaLoading && state.mediaTab != MediaTab.FILE -> item(key = "media-loading") { MediaLoading() }
        pages -> item(key = "media-load-more") {
            LaunchedEffect(state.mediaPagesLoaded, state.mediaTab) { onLoadMore() }
        }
        isEmpty -> item(key = "media-empty") { MediaEmptyState(emptyTextFor(state.mediaTab)) }
    }
    item(key = "media-bottom-space") { Spacer(Modifier.height(24.dp)) }
}

private fun LazyListScope.mediaGrid(
    entries: List<SharedMediaEntry>,
    transfers: Map<String, MediaTransfer>,
    onOpenMedia: (SharedMediaEntry) -> Unit,
    onDownload: (MediaItem) -> Unit,
    onCancelDownload: (itemId: String) -> Unit,
    onCancelUpload: (messageId: String, itemId: String) -> Unit,
) {
    val rows = entries.chunked(GRID_COLUMNS)
    items(rows, key = { row -> "media-row-${row.first().item.id}" }) { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            row.forEach { entry ->
                MediaCell(
                    item = entry.item,
                    transfer = transfers[entry.item.id],
                    isSent = entry.message.status == MessageStatus.SENT,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    onOpen = { onOpenMedia(entry) },
                    onDownload = { onDownload(entry.item) },
                    onCancelUpload = { onCancelUpload(entry.message.id, entry.item.id) },
                    onCancelDownload = { onCancelDownload(entry.item.id) },
                )
            }
            repeat(GRID_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun SharedVoiceRow(
    message: Message.Clip,
    title: String,
    playback: State<ClipPlaybackState>,
    onToggle: () -> Unit,
) {
    val tokens = messengerTokens
    val clip = message.clip as? Clip.Voice ?: return
    val isCurrent by remember(message.id) { derivedStateOf { playback.value.messageId == message.id } }
    val isPlaying by remember(message.id) {
        derivedStateOf { playback.value.let { it.messageId == message.id && it.isPlaying } }
    }
    val isLoading by remember(message.id) {
        derivedStateOf { playback.value.let { it.messageId == message.id && it.isLoading } }
    }
    val failed by remember(message.id) { derivedStateOf { playback.value.failedMessageId == message.id } }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.cardFill)
            .clickable(enabled = !isLoading, onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tokens.accent),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = tokens.onAccent,
                )
                isPlaying -> Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = stringResource(R.string.voice_pause),
                    tint = tokens.onAccent,
                )
                failed -> Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.voice_play_failed),
                    tint = tokens.onAccent,
                )
                else -> Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.voice_play),
                    tint = tokens.onAccent,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = tokens.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            if (isCurrent) {
                VoiceWaveform(
                    waveform = clip.waveform,
                    progress = { playback.value.progress },
                    playedColor = tokens.accent,
                    idleColor = tokens.textMuted.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.profile_media_voice_meta,
                        DateUtils.formatDayDivider(message.timestamp),
                        DateUtils.formatDuration((clip.durationMs / 1000L).toInt()),
                    ),
                    color = tokens.textMuted,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun MediaLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
            color = messengerTokens.accent,
        )
    }
}

@Composable
private fun MediaEmptyState(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        color = messengerTokens.textMuted,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
    )
}

@StringRes
private fun emptyTextFor(tab: MediaTab): Int = when (tab) {
    MediaTab.PHOTO -> R.string.profile_media_empty_photos
    MediaTab.VIDEO -> R.string.profile_media_empty_videos
    MediaTab.VOICE -> R.string.profile_media_empty_voice
    MediaTab.FILE -> R.string.profile_media_empty_files
}

@StringRes
internal fun MediaTab.labelRes(): Int = when (this) {
    MediaTab.PHOTO -> R.string.profile_media_tab_photos
    MediaTab.VIDEO -> R.string.profile_media_tab_videos
    MediaTab.VOICE -> R.string.profile_media_tab_voice
    MediaTab.FILE -> R.string.profile_media_tab_files
}
