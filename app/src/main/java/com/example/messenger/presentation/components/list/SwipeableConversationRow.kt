package com.example.messenger.presentation.components.list

import android.provider.CalendarContract
import android.view.Surface
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.presentation.screens.ui.theme.DangerRed
import com.example.messenger.presentation.screens.ui.theme.LightGray
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip

import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val SwipeActionWidth = 84.dp
private val EditCircle = Color(0xFF8E8E93)

@Composable
fun SwipeableConversationRow(
    displayName: String,
    isOneOnOne: Boolean,
    hasUnread: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onMarkRead: () -> Unit,
    onMarkUnread: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (rowOnClick: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val revealPx = with(LocalDensity.current) { (SwipeActionWidth * 2).toPx() }
    val offsetX = remember { Animatable(0f) }
    var settledOpen by remember { mutableStateOf(false) }
    val progress = { (-offsetX.value / revealPx).coerceIn(0f, 1f) }
    val deleteScale = { (progress() / 0.5f).coerceIn(0f, 1f) }
    val editScale = { ((progress() - 0.5f) / 0.5f).coerceIn(0f, 1f) }

    val close: () -> Unit = {
        scope.launch {
            settledOpen = false
            offsetX.animateTo(0f)
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.End,
        ) {
            ConversationActionButton(
                icon = Icons.Default.Edit,
                contentDescription = "Edit",
                circleColor = EditCircle,
                scale = editScale,
                enabled = settledOpen,
            ) { menuDismiss ->
                DropdownMenu(expanded = true, onDismissRequest = menuDismiss) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { menuDismiss(); close(); onEdit() },
                    )
                    if (hasUnread) {
                        DropdownMenuItem(
                            text = { Text("Mark as read") },
                            onClick = { menuDismiss(); close(); onMarkRead() },
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Mark as unread") },
                            onClick = { menuDismiss(); close(); onMarkUnread() },
                        )
                    }
                }
            }
            ConversationActionButton(
                icon = Icons.Default.Delete,
                contentDescription = "Delete",
                circleColor = DangerRed,
                scale = deleteScale,
                enabled = settledOpen,
            ) { menuDismiss ->
                DropdownMenu(expanded = true, onDismissRequest = menuDismiss) {
                    DropdownMenuItem(
                        text = { Text("Delete for me") },
                        onClick = { menuDismiss(); close(); onDeleteForMe() },
                    )
                    if (isOneOnOne) {
                        DropdownMenuItem(
                            text = { Text("Delete for $displayName too") },
                            onClick = { menuDismiss(); close(); onDeleteForEveryone() },
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val reveal = (-offsetX.value).coerceIn(0f, revealPx).roundToInt()
                    val contentWidth = (constraints.maxWidth - reveal).coerceAtLeast(0)
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = contentWidth, maxWidth = contentWidth),
                    )
                    layout(contentWidth, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            offsetX.snapTo((offsetX.value + delta).coerceIn(-revealPx, 0f))
                        }
                    },
                    onDragStarted = { settledOpen = false },
                    onDragStopped = {
                        scope.launch {
                            val target = if (offsetX.value < -revealPx / 2f) -revealPx else 0f
                            offsetX.animateTo(target)
                            settledOpen = target != 0f
                        }
                    },
                ),
        ) {
            content {
                if (offsetX.value != 0f) close() else onClick()
            }
        }
    }
}

@Composable
private fun ConversationActionButton(
    icon: ImageVector,
    contentDescription: String,
    circleColor: Color,
    scale: () -> Float,
    enabled: Boolean = true,
    menu: @Composable (dismiss: () -> Unit) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(SwipeActionWidth)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    val s = scale()
                    scaleX = s
                    scaleY = s
                }
                .clip(CircleShape)
                .background(circleColor)
                .clickable(enabled = enabled) { menuOpen = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
        if (menuOpen) {
            menu { menuOpen = false }
        }
    }
}
@Preview(showBackground = true, name = "Swipeable Row")
@Composable
private fun SwipeableConversationRowPreview() {
    MessengerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SwipeableConversationRow(
                displayName = "Олександр",
                isOneOnOne = true,
                hasUnread = true,
                onClick = {},
                onEdit = {},
                onMarkRead = {},
                onMarkUnread = {},
                onDeleteForMe = {},
                onDeleteForEveryone = {}
            ) { rowOnClick ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { rowOnClick() }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Олександр: Привіт, як справи?",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Action Button")
@Composable
private fun ConversationActionButtonPreview() {
    MessengerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            // Обертаємо в Box із фіксованою висотою, щоб fillMaxHeight() спрацював
            Box(modifier = Modifier.height(72.dp)) {
                ConversationActionButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "Edit",
                    circleColor = EditCircle,
                    scale = { 1f },
                ) {
                }
            }
        }
    }
}
