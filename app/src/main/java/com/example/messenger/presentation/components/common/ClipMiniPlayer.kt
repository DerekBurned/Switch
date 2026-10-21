package com.example.messenger.presentation.components.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.R
import com.example.messenger.data.media.CLIP_PROGRESS_INTERVAL_MS
import com.example.messenger.data.media.ClipPlaybackState
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.presentation.featureflag.featureEnabled
import com.example.messenger.presentation.theme.MessengerShapes
import com.example.messenger.presentation.theme.messengerTokens
import com.example.messenger.util.DateUtils

val LocalClipBarInset = compositionLocalOf { 0.dp }

fun nextClipSpeed(speed: Float): Float = when {
    speed >= 2f -> 1f
    speed >= 1.5f -> 2f
    else -> 1.5f
}

private fun speedText(speed: Float): String = when {
    speed >= 2f -> "2"
    speed >= 1.5f -> "1.5"
    else -> "1"
}

@Composable
fun ClipMiniPlayer(
    playback: State<ClipPlaybackState>,
    onToggle: () -> Unit,
    onCycleSpeed: () -> Unit,
    onClose: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = messengerTokens
    val shape = MessengerShapes.bubble
    val isPlaying by remember { derivedStateOf { playback.value.isPlaying } }
    val isVideo by remember { derivedStateOf { playback.value.isVideo } }
    val speed by remember { derivedStateOf { playback.value.speed } }
    val positionSeconds by remember { derivedStateOf { (playback.value.positionMs / 1000L).toInt() } }
    val durationSeconds by remember { derivedStateOf { (playback.value.durationMs / 1000L).toInt() } }
    val targetProgress by remember { derivedStateOf { playback.value.progress } }
    val animatedProgress = animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(CLIP_PROGRESS_INTERVAL_MS, easing = LinearEasing),
        label = "clipMiniProgress",
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = shape, clip = false)
            .clip(shape)
            .background(tokens.cardFill)
            .border(1.dp, tokens.panelBorder, shape)
            .clickable { onOpen() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tokens.accent)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(if (isPlaying) R.string.voice_pause else R.string.voice_play),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (isVideo) R.string.clip_video_message else R.string.clip_voice_message),
                    color = tokens.textPrimary,
                    fontSize = 14.sp,
                )
                Text(
                    text = DateUtils.formatDuration(positionSeconds) +
                        " / " + DateUtils.formatDuration(durationSeconds),
                    color = tokens.textMuted,
                    fontSize = 12.sp,
                )
            }
            if (featureEnabled(FeatureFlag.CLIP_PLAYBACK_SPEED)) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.clip_speed, speedText(speed)),
                    color = tokens.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(tokens.accent.copy(alpha = 0.14f))
                        .clickable { onCycleSpeed() }
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.clip_stop_playback),
                tint = tokens.textMuted,
                modifier = Modifier.size(20.dp).clickable { onClose() },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { animatedProgress.value },
            modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
            color = tokens.accent,
            trackColor = tokens.trackFill,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}
