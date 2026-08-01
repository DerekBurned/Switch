package com.example.messenger.presentation.components.call

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.R
import com.example.messenger.data.remote.call.ActiveCallHolder
import com.example.messenger.presentation.components.common.MessengerAvatar

private val AcceptGreen = Color(0xFF34C759)
private val DeclineRed = Color(0xFFE53935)

@Composable
fun IncomingCallBar(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active by ActiveCallHolder.state.collectAsStateWithLifecycle()
    val call = active
    val visible = call != null && call.isIncoming && !call.isActive

    var shown by remember { mutableStateOf<ActiveCallHolder.ActiveCall?>(null) }
    if (visible) shown = call

    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxWidth(),
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
    ) {
        val c = shown ?: return@AnimatedVisibility
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = BarBackground,
            shadowElevation = 8.dp,
        ) {
            val partner = rememberCallPartnerUi(c)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MessengerAvatar(
                    name = partner.displayName,
                    photoUrl = partner.avatarUrl,
                    size = 40.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = partner.displayName.ifBlank { "Incoming call" },
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            if (c.isVideoCall) R.string.call_incoming_video_subtitle else R.string.call_incoming_subtitle,
                        ),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = onDecline,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = DeclineRed,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "Decline")
                }
                run {
                    Spacer(Modifier.width(10.dp))
                    FilledIconButton(
                        onClick = onAccept,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = AcceptGreen,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Accept")
                    }
                }
            }
        }
    }
}
