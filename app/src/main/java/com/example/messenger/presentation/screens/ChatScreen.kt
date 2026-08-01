package com.example.messenger.presentation.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.messenger.domain.model.CallType
import com.example.messenger.domain.model.MediaItem
import com.example.messenger.domain.model.Message
import com.example.messenger.domain.model.MessageStatus
import com.example.messenger.domain.model.PresenceState
import com.example.messenger.presentation.base.ObserveAsEvents
import com.example.messenger.presentation.components.chat.AttachmentBar
import com.example.messenger.presentation.components.call.CallAwareTopBar
import com.example.messenger.presentation.components.chat.ChatMessage
import com.example.messenger.presentation.components.chat.ChatMessageSkeleton
import com.example.messenger.presentation.components.chat.ChatTextBubble
import com.example.messenger.presentation.components.chat.ChatTopBar
import com.example.messenger.presentation.components.chat.LongPressMessage
import com.example.messenger.presentation.components.chat.MessageAction
import com.example.messenger.presentation.components.chat.MessageContextMenuHost
import com.example.messenger.presentation.components.chat.MessageContextMenuState
import com.example.messenger.presentation.components.chat.DateDivider
import com.example.messenger.presentation.components.chat.EndedCallCard
import com.example.messenger.presentation.components.media.FullscreenMediaPager
import com.example.messenger.presentation.components.media.MediaAlbumGrid
import com.example.messenger.presentation.components.media.MediaSource
import com.example.messenger.presentation.components.chat.MissedCallCard
import com.example.messenger.presentation.components.chat.ScrollToBottomButton
import com.example.messenger.presentation.components.chat.SwipeToReply
import com.example.messenger.presentation.components.chat.chatNavigationSwipe
import com.example.messenger.presentation.components.chat.UnreadMessagesDivider
import com.example.messenger.presentation.components.chat.rememberMessageContextMenuState
import com.example.messenger.presentation.components.common.WallpaperBackground
import com.example.messenger.presentation.components.media.mediaCacheFile
import com.example.messenger.presentation.components.common.rememberNavEnterSettled
import com.example.messenger.presentation.effect.ChatEffect
import com.example.messenger.presentation.intent.ChatIntent
import com.example.messenger.presentation.notification.ChatNotifier
import com.example.messenger.presentation.notification.CurrentConversationHolder
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme
import com.example.messenger.presentation.screens.ui.theme.Motion
import com.example.messenger.presentation.screens.ui.theme.PrimaryBlue
import com.example.messenger.presentation.screens.ui.theme.messengerTokens
import com.example.messenger.presentation.state.ChatUiState
import com.example.messenger.presentation.state.GalleryItem
import com.example.messenger.presentation.viewmodel.ChatViewModel
import com.example.messenger.util.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private fun mediaReadPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenWithNav(
    viewModel: ChatViewModel = hiltViewModel(),
    sharedKeyPartnerId: String = "",
    inBubble: Boolean = false,
    onBackClick: () -> Unit = {},
    onCallClick: () -> Unit = {},
    onVideoCallClick: () -> Unit = {},
    onIntercultorProfileClick: (avatarUrl: String?, partnerName: String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var messageText by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current
    ObserveAsEvents(viewModel.effect) { effect ->
        when (effect) {
            is ChatEffect.ShowError -> Toast.makeText(context, effect.message.asString(context), Toast.LENGTH_SHORT).show()
        }
    }

    var showPicker by remember { mutableStateOf(false) }
    var cameraGranted by remember { mutableStateOf(false) }
    var devicePreview by remember { mutableStateOf<Pair<List<GalleryItem>, Int>?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraGranted = granted
        Log.d("ChatScreen", "camera permission granted=$granted")
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val anyGranted = result.values.any { it }
        Log.d("ChatScreen", "media permission result granted=$anyGranted")
        if (anyGranted) showPicker = true
    }

    val conversationId = viewModel.conversationId
    DisposableEffect(conversationId) {
        if (conversationId.isNotBlank()) {
            CurrentConversationHolder.setOpen(conversationId)
            if (!inBubble) ChatNotifier.clear(context, conversationId)
        }
        onDispose {
            if (conversationId.isNotBlank()) {
                CurrentConversationHolder.clear(conversationId)
            }
        }
    }

    ChatScreenContent(
        uiState = uiState,
        sharedKeyPartnerId = sharedKeyPartnerId,
        messageText = messageText,
        onMessageTextChange = { newText ->
            messageText = newText
            viewModel.dispatch(ChatIntent.TextChanged(newText))
        },
        onSendClick = {
            if (messageText.isNotBlank() || uiState.pendingAttachments.isNotEmpty()) {
                viewModel.dispatch(ChatIntent.SendMessage(messageText))
                messageText = ""
            }
        },
        onBackClick = onBackClick,
        onCopy = { text ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
        },
        onReply = { message -> viewModel.dispatch(ChatIntent.SetReplyTo(message)) },
        onReplyClick = { messageId -> viewModel.dispatch(ChatIntent.JumpToMessage(messageId)) },
        onDelete = { message, forEveryone ->
            if (!forEveryone || message.senderId == uiState.currentUserId) {
                viewModel.dispatch(ChatIntent.DeleteMessage(message, forEveryone))
            }
        },
        onClearReply = { viewModel.dispatch(ChatIntent.ClearReply) },
        onAttachmentClick = {
            val perms = mediaReadPermissions()
            val granted = perms.any {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (granted) {
                showPicker = true
            } else {
                mediaPermissionLauncher.launch(perms)
            }
        },
        onClearAttachments = { viewModel.dispatch(ChatIntent.ClearAttachments) },
        onIntercultorProfileClick = onIntercultorProfileClick,
        onCallClick = onCallClick,
        onVideoCallClick = onVideoCallClick,
        onMessagesSeen = { messages -> viewModel.dispatch(ChatIntent.MessagesSeen(messages)) },
        onLoadOlderMessages = { viewModel.dispatch(ChatIntent.LoadOlderMessages) },
        onDownload = { item -> viewModel.dispatch(ChatIntent.DownloadMedia(item)) },
        onCancelUpload = { messageId, itemId -> viewModel.dispatch(ChatIntent.CancelUpload(messageId, itemId)) },
        onCancelDownload = { itemId -> viewModel.dispatch(ChatIntent.CancelDownload(itemId)) },
        onRetryMedia = { messageId -> viewModel.dispatch(ChatIntent.RetryMedia(messageId)) },
    )

    if (showPicker) {
        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                cameraGranted = true
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        MediaPickerBottomSheet(
            selected = uiState.pendingAttachments.map { it.uri }.toSet(),
            onToggle = { uri, kind -> viewModel.dispatch(ChatIntent.ToggleAttachment(uri, kind)) },
            onPreview = { items, index -> devicePreview = items to index },
            onCaptured = { uri, kind -> viewModel.dispatch(ChatIntent.AddAttachment(uri, kind)) },
            onDone = { showPicker = false },
            onDismiss = { showPicker = false },
            cameraEnabled = cameraGranted,
        )
    }

    devicePreview?.let { (items, index) ->
        val sources = remember(items) {
            items.map { gi ->
                MediaSource(
                    key = gi.id.toString(),
                    isVideo = gi.kind == MediaItem.VIDEO,
                    model = gi.uri,
                    playUri = gi.uri,
                )
            }
        }
        FullscreenMediaPager(
            sources = sources,
            startIndex = index,
            onDismiss = { devicePreview = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreenContent(
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
) {
    val context = LocalContext.current
    val tokens = messengerTokens
    val enterSettled = rememberNavEnterSettled()
    var chatPreviewIndex by remember { mutableStateOf<Int?>(null) }
    val presenceStatusText = when {
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
                    delay(220L)
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

            MessageComposer(
                messageText = messageText,
                uiState = uiState,
                onMessageTextChange = onMessageTextChange,
                onSendClick = onSendClick,
                onAttachmentClick = onAttachmentClick,
                onClearAttachments = onClearAttachments,
                onClearReply = onClearReply,
                onHeightChanged = { composerHeightPx = it },
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

@Composable
private fun MessageComposer(
    messageText: String,
    uiState: ChatUiState,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    onClearAttachments: () -> Unit,
    onClearReply: () -> Unit,
    onHeightChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = messengerTokens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { onHeightChanged(it.height) },
    ) {
        AttachmentBar(
            attachments = uiState.pendingAttachments,
            onClear = onClearAttachments,
            modifier = Modifier.fillMaxWidth(),
        )

        val replyingTo = uiState.replyingTo
        if (replyingTo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tokens.fieldFill)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = null,
                    tint = tokens.accent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (replyingTo) {
                        is Message.Text  -> replyingTo.text
                        is Message.Media -> replyingTo.caption.ifBlank { "Media" }
                        is Message.Call  -> "Call"
                    },
                    color = tokens.textOnField,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onClearReply,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel reply",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        val composerShape = RoundedCornerShape(28.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .shadow(elevation = 10.dp, shape = composerShape, clip = false)
                .clip(composerShape)
                .background(tokens.cardFill)
                .border(1.dp, tokens.panelBorder, composerShape)
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onAttachmentClick,
                modifier = Modifier.size(42.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "Add attachment",
                    tint = tokens.accent,
                    modifier = Modifier.size(26.dp),
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(tokens.fieldFill)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (messageText.isEmpty()) {
                    Text(
                        text = "Message",
                        color = tokens.textOnField.copy(alpha = 0.45f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                BasicTextField(
                    value = messageText,
                    onValueChange = onMessageTextChange,
                    cursorBrush = SolidColor(tokens.accent),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = tokens.textOnField),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            val hasContent = messageText.isNotBlank() || uiState.pendingAttachments.isNotEmpty()
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(tokens.accent)
                    .clickable(enabled = !uiState.isSending) {
                        if (hasContent) onSendClick()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (hasContent) Icons.AutoMirrored.Outlined.Send else Icons.Default.Mic,
                    contentDescription = if (hasContent) "Send" else "Record voice message",
                    tint = tokens.onAccent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    msg: Message,
    isMe: Boolean,
    uiState: ChatUiState,
    ctxState: MessageContextMenuState,
    onReply: (Message) -> Unit,
    onCopy: (String) -> Unit,
    onReplyClick: (messageId: String) -> Unit,
    onDelete: (Message, Boolean) -> Unit,
    onDownload: (MediaItem) -> Unit,
    onCancelUpload: (messageId: String, itemId: String) -> Unit,
    onCancelDownload: (itemId: String) -> Unit,
    onRetryMedia: (messageId: String) -> Unit,
    onCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onOpenMedia: (index: Int) -> Unit,
) {
    val tokens = messengerTokens
    when (msg) {
        is Message.Call -> {
            val noop: () -> Unit = {}
            val callActions = buildList {
                if (isMe) add(MessageAction("Delete for everyone", Icons.Default.Delete, color = Color.Red) { onDelete(msg, true) })
                add(MessageAction("Delete for me", Icons.Default.Delete, color = Color.Red) { onDelete(msg, false) })
            }
            val callCard: @Composable (live: Boolean) -> Unit = { live ->
                val onCall = when {
                    !live -> noop
                    msg.video -> onVideoCallClick
                    else -> onCallClick
                }
                when (msg.callType) {
                    CallType.MISSED -> MissedCallCard(
                        onCall = onCall,
                        timestamp = msg.timestamp,
                        isMe = isMe,
                        title = if (msg.video) "Missed video call" else "Missed Call",
                        status = msg.status,
                        video = msg.video,
                    )
                    CallType.UNREACHED -> MissedCallCard(
                        onCall = onCall,
                        timestamp = msg.timestamp,
                        isMe = isMe,
                        title = if (msg.video) "Unreached video call" else "Unreached call",
                        status = msg.status,
                        video = msg.video,
                    )
                    CallType.ENDED -> EndedCallCard(
                        durationSeconds = msg.durationSeconds,
                        timestamp = msg.timestamp,
                        isMe = isMe,
                        video = msg.video,
                    )
                }
            }
            LongPressMessage(
                isMe = isMe,
                messageKey = msg.id,
                actions = callActions,
                state = ctxState,
                preview = { callCard(false) },
            ) { callCard(true) }
        }
        is Message.Media -> {
            val mediaActions = buildList {
                add(MessageAction("Reply", Icons.AutoMirrored.Filled.Reply, color = tokens.textPrimary) { onReply(msg) })
                if (isMe) add(MessageAction("Delete for everyone", Icons.Default.Delete, color = Color.Red) { onDelete(msg, true) })
                add(MessageAction("Delete for me", Icons.Default.Delete, color = Color.Red) { onDelete(msg, false) })
            }
            SwipeToReply(onReply = { onReply(msg) }) {
                LongPressMessage(
                    isMe = isMe,
                    messageKey = msg.id,
                    actions = mediaActions,
                    state = ctxState,
                    preview = {
                        MediaAlbumGrid(
                            message = msg,
                            isMe = isMe,
                            transfers = uiState.transfers,
                            onOpen = {},
                            onDownload = {},
                            onCancelUpload = { _, _ -> },
                            onCancelDownload = {},
                        )
                    },
                ) {
                    MediaAlbumGrid(
                        message = msg,
                        isMe = isMe,
                        transfers = uiState.transfers,
                        onOpen = { item ->
                            if (msg.status == MessageStatus.FAILED) {
                                onRetryMedia(msg.id)
                            } else {
                                val idx = uiState.conversationMedia
                                    .indexOfFirst { it.id == item.id }
                                if (idx >= 0) onOpenMedia(idx)
                            }
                        },
                        onDownload = { onDownload(it) },
                        onCancelUpload = { messageId, itemId -> onCancelUpload(messageId, itemId) },
                        onCancelDownload = { onCancelDownload(it) },
                    )
                }
            }
        }
        is Message.Text -> {
            val chatMessage = ChatMessage(
                text = msg.text,
                isMe = isMe,
                status = msg.status,
                timestamp = msg.timestamp,
                replyToMessageId = msg.replyToMessageId,
                replyToText = msg.replyToText,
                replyToSenderLabel = msg.replyToMessageId?.let {
                    if (msg.replyToSenderId == uiState.currentUserId) "You"
                    else uiState.partnerUsername.ifBlank { "User" }
                },
            )
            val textActions = buildList {
                add(MessageAction("Copy", Icons.Default.ContentCopy, color = tokens.textPrimary) { onCopy(msg.text) })
                add(MessageAction("Reply", Icons.AutoMirrored.Filled.Reply, color = tokens.textPrimary) { onReply(msg) })
                if (isMe) add(MessageAction("Delete for everyone", Icons.Default.Delete, color = Color.Red) { onDelete(msg, true) })
                add(MessageAction("Delete for me", Icons.Default.Delete, color = Color.Red) { onDelete(msg, false) })
            }
            SwipeToReply(onReply = { onReply(msg) }) {
                LongPressMessage(
                    isMe = isMe,
                    messageKey = msg.id,
                    actions = textActions,
                    state = ctxState,
                    preview = {
                        ChatTextBubble(message = chatMessage, highlighted = false, onReplyClick = {})
                    },
                ) {
                    ChatTextBubble(
                        message = chatMessage,
                        highlighted = msg.id == uiState.highlightedMessageId,
                        onReplyClick = { msg.replyToMessageId?.let(onReplyClick) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageAppearance(
    messageId: String,
    isMe: Boolean,
    exiting: Boolean,
    animatedIds: MutableSet<String>,
    content: @Composable () -> Unit,
) {
    val isNew = remember(messageId) { messageId !in animatedIds }
    val progress = remember(messageId) { Animatable(if (isNew) 0f else 1f) }
    val slidePx = with(LocalDensity.current) { 120.dp.toPx() }

    LaunchedEffect(messageId) {
        if (isNew) {
            animatedIds.add(messageId)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
    }
    LaunchedEffect(exiting) {
        if (exiting) {
            progress.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 200))
        }
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                val p = progress.value
                alpha = p.coerceIn(0f, 1f)
                if (exiting) {
                    transformOrigin = TransformOrigin(if (isMe) 1f else 0f, 0f)
                    scaleX = p
                    scaleY = p
                } else {
                    translationX = (1f - p) * slidePx * if (isMe) 1f else -1f
                    val s = 0.7f + 0.3f * p
                    scaleX = s
                    scaleY = s
                }
            }
            .then(
                if (exiting) {
                    Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val h = (placeable.height * progress.value).roundToInt().coerceAtLeast(0)
                        layout(placeable.width, h) { placeable.place(0, 0) }
                    }
                } else {
                    Modifier
                }
            ),
    ) {
        content()
    }
}

private sealed interface ChatRow {
    data class MessageRow(val message: Message) : ChatRow
    data object UnreadDivider : ChatRow
    data class DateDivider(val dayStart: Long, val label: String) : ChatRow
}

@Preview(showBackground = true, showSystemUi = true, name = "preview")
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
            )
        }
    }
}
