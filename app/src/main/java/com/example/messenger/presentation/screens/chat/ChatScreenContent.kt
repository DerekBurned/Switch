package com.example.messenger.presentation.screens.chat

import android.view.TextureView
import androidx.camera.core.Preview
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.messenger.data.media.ClipPlaybackState
import com.example.messenger.data.media.VideoRecordState
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.presence.PresenceState
import com.example.messenger.presentation.components.call.CallAwareTopBar
import com.example.messenger.presentation.components.chat.ChatMessageSkeleton
import com.example.messenger.presentation.components.chat.ChatTopBar
import com.example.messenger.presentation.components.chat.DateDivider
import com.example.messenger.presentation.components.chat.MessageContextMenuHost
import com.example.messenger.presentation.components.chat.ScrollToBottomButton
import com.example.messenger.presentation.components.chat.UnreadMessagesDivider
import com.example.messenger.presentation.components.chat.VideoNoteRecordingOverlay
import com.example.messenger.presentation.components.chat.chatNavigationSwipe
import com.example.messenger.presentation.components.chat.rememberMessageContextMenuState
import com.example.messenger.presentation.components.common.WallpaperBackground
import com.example.messenger.presentation.components.common.rememberNavEnterSettled
import com.example.messenger.presentation.components.media.FullscreenMediaPager
import com.example.messenger.presentation.components.media.mediaCacheFile
import com.example.messenger.presentation.components.media.model.MediaSource
import com.example.messenger.presentation.featureflag.featureEnabled
import com.example.messenger.presentation.theme.MessengerTheme
import com.example.messenger.presentation.theme.Motion
import com.example.messenger.presentation.theme.PrimaryBlue
import com.example.messenger.presentation.theme.messengerTokens
import com.example.messenger.util.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatScreenContent(
    uiState: ChatUiState,
    sharedKeyPartnerId: String = "",
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onBackClick: () -> Unit,
    onCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onIntercultorProfileClick: (avatarUrl: String?, partnerName: String) -> Unit,
    onCopy: (String) -> Unit,
    onReply: (Message) -> Unit,
    onReplyClick: (messageId: String) -> Unit,
    onDelete: (Message, Boolean) -> Unit,
    onClearReply: () -> Unit,
    onAttachmentClick: () -> Unit,
    onClearAttachments: () -> Unit,
    onMessagesSeen: (List<Message>) -> Unit,
    onLoadOlderMessages: () -> Unit,
    onDownload: (MediaItem) -> Unit,
    onCancelUpload: (messageId: String, itemId: String) -> Unit,
    onCancelDownload: (itemId: String) -> Unit,
    onRetryMedia: (messageId: String) -> Unit,
    onPlayClip: (String) -> Unit,
    onSeekClip: (Float) -> Unit,
    clipPlayback: State<ClipPlaybackState>,
    onRetryClip: (String) -> Unit,
    onCancelClipSend: (String) -> Unit,
    onStartRecording: () -> Unit,
    onLockRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onFinishRecording: () -> Unit,
    onInterruptRecording: () -> Unit,
    onSendVoiceDraft: () -> Unit,
    onDiscardVoiceDraft: () -> Unit,
    onToggleClipMode: () -> Unit = {},
    onAttachVideoPreview: (Preview.SurfaceProvider) -> Unit = {},
    onDetachVideoPreview: (Preview.SurfaceProvider) -> Unit = {},
    videoFrame: suspend (String) -> File? = { null },
    onAttachClipVideo: (TextureView) -> Unit = {},
    onDetachClipVideo: (TextureView) -> Unit = {},
) {
    val context = LocalContext.current
    val tokens = messengerTokens
    val enterSettled = rememberNavEnterSettled()
    var chatPreviewIndex by remember { mutableStateOf<Int?>(null) }
    val presenceStatusText = when {
        uiState.partnerDeleted -> DateUtils.LAST_SEEN_DELETED
        uiState.isPartnerTyping -> "typing..."
        uiState.partnerPresence.state == PresenceState.ONLINE -> "Online"
        uiState.partnerPresence.state == PresenceState.AWAY -> "Away"
        else -> uiState.partnerLastSeenDisplay.ifBlank {
            DateUtils.formatLastSeen(uiState.partnerPresence.lastSeen)
        }
    }

    val ctxState = rememberMessageContextMenuState()
    MessageContextMenuHost(state = ctxState) {
    Scaffold(
        topBar = {
            CallAwareTopBar {
                ChatTopBar(
                    partnerName = uiState.partnerUsername,
                    presenceStatusText = presenceStatusText,
                    statusAccent = uiState.isPartnerTyping ||
                        uiState.partnerPresence.state == PresenceState.ONLINE,
                    presenceState = uiState.partnerPresence.state,
                    isTyping = uiState.isPartnerTyping,
                    sharedKeyPartnerId = sharedKeyPartnerId,
                    onBackClick = onBackClick,
                    onProfileClick = { onIntercultorProfileClick(uiState.partnerAvatarUrl,
                        uiState.partnerUsername) },
                    onCallClick = onCallClick,
                    onVideoCallClick = onVideoCallClick,
                    partnerAvatarUrl = uiState.partnerAvatarUrl,
                    partnerDeleted = uiState.partnerDeleted,
                )
            }
        },
        containerColor = Color.Transparent,
    ) { padding ->
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        val animatedIds = remember { mutableSetOf<String>() }
        val exitingIds = remember { mutableStateListOf<String>() }
        val requestDelete: (Message, Boolean) -> Unit = { message, forEveryone ->
            if (message.id !in exitingIds) {
                exitingIds.add(message.id)
                coroutineScope.launch {
                    delay(220L.milliseconds)
                    onDelete(message, forEveryone)
                }
            }
        }
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        val isAtBottom by remember {
            derivedStateOf {
                val info = listState.layoutInfo
                val total = info.totalItemsCount
                total == 0 || (info.visibleItemsInfo.lastOrNull()?.index ?: -1) >= total - 1
            }
        }

        var unreadCount by rememberSaveable { mutableStateOf(0) }
        var didInitialScroll by remember { mutableStateOf(false) }
        var lastKnownNewestId by remember { mutableStateOf<String?>(null) }

        var dividerAnchorId by remember { mutableStateOf<String?>(null) }
        var seededReopenAnchor by remember { mutableStateOf(false) }

        if (!seededReopenAnchor && uiState.unreadAnchorResolved) {
            dividerAnchorId = uiState.firstUnreadMessageId
            seededReopenAnchor = true
        }

        var seededAppear by remember { mutableStateOf(false) }
        if (!seededAppear && uiState.messages.isNotEmpty()) {
            uiState.messages.forEach { animatedIds.add(it.id) }
            seededAppear = true
        }

        val rows = remember(uiState.messages, dividerAnchorId) {
            buildList<ChatRow> {
                var lastDayStart = Long.MIN_VALUE
                uiState.messages.forEach { message ->
                    val dayStart = DateUtils.startOfDay(message.timestamp)
                    if (dayStart != lastDayStart) {
                        add(ChatRow.DateDivider(dayStart, DateUtils.formatDayDivider(message.timestamp)))
                        lastDayStart = dayStart
                    }
                    if (message.id == dividerAnchorId) add(ChatRow.UnreadDivider)
                    add(ChatRow.MessageRow(message))
                }
            }
        }

        LaunchedEffect(rows, didInitialScroll) {
            if (didInitialScroll || rows.isEmpty()) return@LaunchedEffect
            val dividerIndex = rows.indexOfFirst { it is ChatRow.UnreadDivider }
            if (dividerIndex >= 0) {
                listState.scrollToItem(dividerIndex)
            } else {
                listState.scrollToItem(rows.size - 1)
            }
            lastKnownNewestId = uiState.messages.lastOrNull()?.id
            didInitialScroll = true
        }

        LaunchedEffect(uiState.highlightedMessageId, rows) {
            val id = uiState.highlightedMessageId ?: return@LaunchedEffect
            val index = rows.indexOfFirst { it is ChatRow.MessageRow && it.message.id == id }
            if (index >= 0) listState.animateScrollToItem(index)
        }

        LaunchedEffect(uiState.messages, didInitialScroll) {
            if (!didInitialScroll) return@LaunchedEffect
            val newest = uiState.messages.lastOrNull() ?: return@LaunchedEffect
            if (newest.id == lastKnownNewestId) return@LaunchedEffect
            val prevNewestId = lastKnownNewestId
            lastKnownNewestId = newest.id
            if (prevNewestId == null) return@LaunchedEffect
            if (isAtBottom || newest.senderId == uiState.currentUserId) {
                listState.animateScrollToItem(rows.size - 1)
            } else {
                val newOnes = uiState.messages
                    .dropWhile { it.id != prevNewestId }
                    .drop(1)
                    .filter { it.senderId != uiState.currentUserId }
                unreadCount += newOnes.size
                if (dividerAnchorId == null) {
                    newOnes.firstOrNull()?.let { dividerAnchorId = it.id }
                }
            }
        }

        LaunchedEffect(isAtBottom) {
            if (isAtBottom) {
                unreadCount = 0
                
                dividerAnchorId = null
            }
        }

        val shouldLoadOlder by remember {
            derivedStateOf {
                val info = listState.layoutInfo
                val first = info.visibleItemsInfo.firstOrNull()?.index ?: 0
                info.totalItemsCount > 0 && first <= 2
            }
        }
        LaunchedEffect(shouldLoadOlder, didInitialScroll) {
            if (didInitialScroll && shouldLoadOlder && uiState.hasMoreOlder && !uiState.isLoadingOlder) {
                onLoadOlderMessages()
            }
        }

        val messagesById = remember(uiState.messages) { uiState.messages.associateBy { it.id } }
        LaunchedEffect(listState, messagesById, uiState.currentUserId) {
            snapshotFlow {
                val info = listState.layoutInfo
                val viewportTop = info.viewportStartOffset
                val viewportBottom = info.viewportEndOffset
                info.visibleItemsInfo
                    .asSequence()
                    .filter { item ->
                        val size = item.size
                        if (size <= 0) return@filter false
                        val top = item.offset
                        val bottom = top + size
                        val visiblePx = (minOf(bottom, viewportBottom) - maxOf(top, viewportTop))
                            .coerceAtLeast(0)
                        visiblePx.toFloat() / size >= 1f / 4f
                    }
                    .mapNotNull { item -> messagesById[item.key as? String] }
                    .filter { it.senderId != uiState.currentUserId && it.status != MessageStatus.READ }
                    .toList()
            }
                .distinctUntilChanged { old, new ->
                    old.mapTo(HashSet()) { it.id } == new.mapTo(HashSet()) { it.id }
                }
                .collect { messages -> if (messages.isNotEmpty()) onMessagesSeen(messages) }
        }

        val density = LocalDensity.current
        var composerHeightPx by remember { mutableStateOf(0) }
        val composerHeight = with(density) { composerHeightPx.toDp() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .chatNavigationSwipe(
                    enabled = featureEnabled(FeatureFlag.CHAT_SWIPE_NAVIGATION),
                    onSwipeRight = onBackClick,
                    onSwipeLeft = { onIntercultorProfileClick(uiState.partnerAvatarUrl, uiState.partnerUsername) },
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
            when {
                !enterSettled || (uiState.isLoading && uiState.messages.isEmpty()) -> {
                    ChatMessageSkeleton(modifier = Modifier.weight(1f).fillMaxWidth())
                }
                uiState.error != null && uiState.messages.isEmpty() -> {
                    val errorMessage = uiState.error.asString()
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = errorMessage, color = Color.Red)
                    }
                }
                uiState.messages.isEmpty() -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No messages yet — say hi",
                            color = tokens.textPrimary.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                })
                            }
                    ) {
                        val topPaddingPx = with(density) { (padding.calculateTopPadding() + 75.dp).toPx() }
                        val bottomPaddingPx = composerHeightPx.toFloat() + with(density) { 75.dp.toPx() }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                .drawWithContent {
                                drawContent()

                                val topStop = (topPaddingPx / size.height).coerceIn(0f, 1f)
                                val bottomStop = ((size.height - bottomPaddingPx) / size.height).coerceIn(0f, 1f)

                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        topStop to Color.Black,
                                        bottomStop to Color.Black,
                                        1f to Color.Transparent,
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            },
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = padding.calculateTopPadding() + 8.dp,
                                bottom = composerHeight + 8.dp
                            ),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            items(
                                items = rows,
                                key = { row ->
                                    when (row) {
                                        is ChatRow.MessageRow -> row.message.id
                                        ChatRow.UnreadDivider -> "unread_divider"
                                        is ChatRow.DateDivider -> "date_${row.dayStart}"
                                    }
                                },
                            ) { row ->
                                when (row) {
                                    ChatRow.UnreadDivider -> UnreadMessagesDivider()
                                    is ChatRow.DateDivider -> DateDivider(label = row.label)
                                    is ChatRow.MessageRow -> {
                                        val msg = row.message
                                        val isMe = msg.senderId == uiState.currentUserId
                                        MessageAppearance(
                                            messageId = msg.id,
                                            isMe = isMe,
                                            exiting = msg.id in exitingIds,
                                            animatedIds = animatedIds,
                                        ) {
                                        Column {
                                        MessageBubble(
                                            msg = msg,
                                            isMe = isMe,
                                            uiState = uiState,
                                            ctxState = ctxState,
                                            onReply = onReply,
                                            onCopy = onCopy,
                                            onReplyClick = onReplyClick,
                                            onDelete = requestDelete,
                                            onDownload = onDownload,
                                            onCancelUpload = onCancelUpload,
                                            onCancelDownload = onCancelDownload,
                                            onRetryMedia = onRetryMedia,
                                            onCallClick = onCallClick,
                                            onVideoCallClick = onVideoCallClick,
                                            onOpenMedia = { index -> chatPreviewIndex = index },
                                            onPlayClip = onPlayClip,
                                            onSeekClip = onSeekClip,
                                            clipPlayback = clipPlayback,
                                            onRetryClip = onRetryClip,
                                            onCancelClipSend = onCancelClipSend,
                                            videoFrame = videoFrame,
                                            onAttachVideo = onAttachClipVideo,
                                            onDetachVideo = onDetachClipVideo,
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        }
                                        }
                                    }
                                }
                            }
                        }

                        if (uiState.isLoadingOlder) {
                            CircularProgressIndicator(
                                color = PrimaryBlue,
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = padding.calculateTopPadding() + 8.dp)
                                    .size(28.dp),
                            )
                        }
                    }
                }
            }
            }

            AnimatedVisibility(
                visible = !isAtBottom,
                enter = fadeIn() + scaleIn(initialScale = 0.6f),
                exit = fadeOut() + scaleOut(targetScale = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = composerHeight + 12.dp),
            ) {
                ScrollToBottomButton(
                    unreadCount = unreadCount,
                    onClick = {
                        coroutineScope.launch {
                            val target = rows.lastIndex
                            if (target >= 0) {
                                val animatedTail = 20
                                if (target - listState.firstVisibleItemIndex > animatedTail) {
                                    listState.scrollToItem(target - animatedTail)
                                }
                                listState.animateScrollToItem(target)
                            }
                            unreadCount = 0
                        }
                    },
                )
            }

            val videoRecording = uiState.videoRecording as? VideoRecordState.Recording
            var lastVideoRecording by remember { mutableStateOf<VideoRecordState.Recording?>(null) }
            if (videoRecording != null) lastVideoRecording = videoRecording
            androidx.compose.animation.AnimatedVisibility(
                visible = videoRecording != null,
                enter = fadeIn(tween(Motion.durationShort)),
                exit = fadeOut(tween(Motion.durationShort)),
                modifier = Modifier.fillMaxSize(),
            ) {
                lastVideoRecording?.let { shown ->
                    VideoNoteRecordingOverlay(
                        state = shown,
                        onAttachPreview = onAttachVideoPreview,
                        onDetachPreview = onDetachVideoPreview,
                        bottomInset = composerHeight,
                    )
                }
            }

            MessageComposer(
                messageText = messageText,
                uiState = uiState,
                onToggleClipMode = onToggleClipMode,
                onMessageTextChange = onMessageTextChange,
                onSendClick = onSendClick,
                onAttachmentClick = onAttachmentClick,
                onClearAttachments = onClearAttachments,
                onClearReply = onClearReply,
                onHeightChanged = { composerHeightPx = it },
                onStartRecording = onStartRecording,
                onLockRecording = onLockRecording,
                onCancelRecording = onCancelRecording,
                onFinishRecording = onFinishRecording,
                onInterruptRecording = onInterruptRecording,
                onSendVoiceDraft = onSendVoiceDraft,
                onDiscardVoiceDraft = onDiscardVoiceDraft,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
    }

    var lastPreviewIndex by remember { mutableStateOf(0) }
    chatPreviewIndex?.let { lastPreviewIndex = it }
    androidx.compose.animation.AnimatedVisibility(
        visible = chatPreviewIndex != null,
        enter = androidx.compose.animation.fadeIn(tween(Motion.durationShort)) +
            androidx.compose.animation.scaleIn(
                initialScale = 0.88f,
                animationSpec = tween(Motion.durationMedium, easing = Motion.emphasized),
            ),
        exit = androidx.compose.animation.fadeOut(tween(Motion.durationShort)) +
            androidx.compose.animation.scaleOut(targetScale = 0.92f),
    ) {
        val media = uiState.conversationMedia
        val sources = remember(media) {
            media.map { item ->
                val file = mediaCacheFile(context, item)
                MediaSource(
                    key = item.id,
                    isVideo = item.kind == MediaItem.VIDEO,
                    model = file,
                    playUri = android.net.Uri.fromFile(file),
                    blurHash = item.blurHash,
                )
            }
        }
        androidx.compose.runtime.key(lastPreviewIndex) {
            FullscreenMediaPager(
                sources = sources,
                startIndex = lastPreviewIndex,
                onDismiss = { chatPreviewIndex = null },
                onPageSettled = { idx ->
                    media.getOrNull(idx)?.let { item ->
                        val file = mediaCacheFile(context, item)
                        if ((!file.exists() || file.length() == 0L) && item.url.isNotBlank()) {
                            onDownload(item)
                        }
                    }
                },
            )
        }
    }
}

private sealed interface ChatRow {
    data class MessageRow(val message: Message) : ChatRow
    data object UnreadDivider : ChatRow
    data class DateDivider(val dayStart: Long, val label: String) : ChatRow
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, showSystemUi = true, name = "preview")
@Composable
private fun ChatScreenPreview() {
    val fakeMessages = listOf(
        Message.Text(id = "1", senderId = "me", text = "Hey there!", timestamp = 1_700_000_000_000L, status = MessageStatus.READ),
        Message.Text(id = "2", senderId = "partner", text = "Hi! How are you?", timestamp = 1_700_000_060_000L, status = MessageStatus.SENT),
        Message.Text(id = "3", senderId = "me", text = "All good, thanks!", timestamp = 1_700_000_120_000L, status = MessageStatus.SENT),
    )
    MessengerTheme {
        WallpaperBackground {
            ChatScreenContent(
                uiState = ChatUiState(
                    messages = fakeMessages,
                    partnerUsername = "Alice",
                    isLoading = false,
                    currentUserId = "me",
                ),
                messageText = "",
                onMessageTextChange = {},
                onSendClick = {},
                onBackClick = {},
                onCallClick = {},
                onVideoCallClick = {},
                onCopy = {},
                onReply = {},
                onReplyClick = {},
                onDelete = { _, _ -> },
                onClearReply = {},
                onAttachmentClick = {},
                onClearAttachments = {},
                onIntercultorProfileClick = { _, _ -> },
                onMessagesSeen = {},
                onLoadOlderMessages = {},
                onDownload = {},
                onCancelUpload = { _, _ -> },
                onCancelDownload = {},
                onRetryMedia = {},
                onPlayClip = {},
                onSeekClip = {},
                clipPlayback = remember { mutableStateOf(ClipPlaybackState()) },
                onRetryClip = {},
                onCancelClipSend = {},
                onStartRecording = {},
                onLockRecording = {},
                onCancelRecording = {},
                onFinishRecording = {},
                onInterruptRecording = {},
                onSendVoiceDraft = {},
                onDiscardVoiceDraft = {},
            )
        }
    }
}
