package com.example.messenger.presentation.components.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.R
import com.example.messenger.presentation.theme.messengerTokens
import com.example.messenger.util.DateUtils

@Composable
fun RecordingBar(
    elapsedMs: Long,
    amplitudes: List<Int>?,
    sampleCount: Int,
    locked: Boolean,
    onCancel: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    dragX: () -> Float = { 0f },
    cancelProgress: () -> Float = { 0f },
) {
    val tokens = messengerTokens
    val transition = rememberInfiniteTransition(label = "recordPulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "recordPulseAlpha",
    )
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .alpha(pulse)
                .clip(CircleShape)
                .background(tokens.danger),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = DateUtils.formatDuration((elapsedMs / 1000L).toInt()),
            color = tokens.textPrimary,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.width(10.dp))
        if (amplitudes != null) {
            LiveRecordingWaveform(
                amplitudes = amplitudes,
                sampleCount = sampleCount,
                color = tokens.accent,
                modifier = Modifier.weight(1f).height(26.dp),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.width(10.dp))
        if (locked) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.voice_discard_recording),
                tint = tokens.danger,
                modifier = Modifier.size(22.dp).clickable { onCancel() },
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = stringResource(R.string.voice_stop_recording),
                tint = tokens.accent,
                modifier = Modifier.size(24.dp).clickable { onStop() },
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .graphicsLayer {
                        translationX = dragX().coerceAtMost(0f)
                        alpha = 1f - cancelProgress()
                    }
                    .background(tokens.backgroundBottom)
                    .background(tokens.cardFill)
                    .padding(start = 4.dp, end = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = tokens.textMuted,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.voice_slide_to_cancel),
                    color = tokens.textMuted,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
