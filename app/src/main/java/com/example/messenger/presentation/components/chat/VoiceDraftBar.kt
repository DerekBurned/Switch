package com.example.messenger.presentation.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.messenger.R
import com.example.messenger.domain.model.voicemessage.ClipKind
import com.example.messenger.presentation.theme.messengerTokens
import com.example.messenger.util.DateUtils
import java.io.File

@Composable
fun VoiceDraftBar(
    durationMs: Long,
    waveform: List<Int>,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ClipKind = ClipKind.VOICE,
    file: File? = null,
) {
    val tokens = messengerTokens
    val isVideo = kind == ClipKind.VIDEO
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(if (isVideo) R.string.video_discard_draft else R.string.voice_discard_draft),
            tint = tokens.danger,
            modifier = Modifier.size(22.dp).clickable { onDiscard() },
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.voice_draft),
            color = tokens.textMuted,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = DateUtils.formatDuration((durationMs / 1000L).toInt()),
            color = tokens.textPrimary,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.width(10.dp))
        if (isVideo) {
            AsyncImage(
                model = file,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tokens.fieldFill),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.clip_video_message),
                color = tokens.textPrimary,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
        } else {
            VoiceWaveform(
                waveform = waveform,
                progress = { 0f },
                playedColor = tokens.accent,
                idleColor = tokens.accent.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f).height(26.dp),
            )
        }
    }
}
