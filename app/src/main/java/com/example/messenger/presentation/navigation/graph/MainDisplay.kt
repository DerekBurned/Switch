package com.example.messenger.presentation.navigation.graph

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.messenger.data.remote.call.ActiveCallHolder
import com.example.messenger.data.remote.call.CallForegroundService
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.presentation.components.call.ActiveCallBar
import com.example.messenger.presentation.components.call.CallBarOverlay
import com.example.messenger.presentation.components.call.CallBarPresenter
import com.example.messenger.presentation.components.call.IncomingCallBar
import com.example.messenger.presentation.components.call.LocalCallBarInset
import com.example.messenger.presentation.components.call.LocalOpenActiveCall
import com.example.messenger.presentation.components.call.VideoSurfaceOwner
import com.example.messenger.presentation.components.call.isCallBarVisible
import com.example.messenger.presentation.components.call.model.CallBarMode
import com.example.messenger.presentation.components.common.ClipMiniPlayer
import com.example.messenger.presentation.components.common.ClipPlayerViewModel
import com.example.messenger.presentation.components.common.FloatingTabBar
import com.example.messenger.presentation.components.common.LocalClipBarInset
import com.example.messenger.presentation.components.common.LocalSharedTransitionScope
import com.example.messenger.presentation.components.common.model.MainTab
import com.example.messenger.presentation.components.common.nextClipSpeed
import com.example.messenger.presentation.components.search.SearchResultsOverlay
import com.example.messenger.presentation.featureflag.featureEnabled
import com.example.messenger.presentation.navigation.AppearanceRoute
import com.example.messenger.presentation.navigation.AuthRoute
import com.example.messenger.presentation.navigation.CallRoute
import com.example.messenger.presentation.navigation.CallsRoute
import com.example.messenger.presentation.navigation.ChangeAccountRoute
import com.example.messenger.presentation.navigation.ChangePhoneRoute
import com.example.messenger.presentation.navigation.ChatRoute
import com.example.messenger.presentation.navigation.ChatUserProfileRoute
import com.example.messenger.presentation.navigation.ChatsRoute
import com.example.messenger.presentation.navigation.ContactsRoute
import com.example.messenger.presentation.navigation.DataStorageRoute
import com.example.messenger.presentation.navigation.DeleteAccountRoute
import com.example.messenger.presentation.navigation.EditChatRoute
import com.example.messenger.presentation.navigation.EditContactDataRoute
import com.example.messenger.presentation.navigation.EditProfileRoute
import com.example.messenger.presentation.navigation.LanguageRoute
import com.example.messenger.presentation.navigation.NotificationsRoute
import com.example.messenger.presentation.navigation.PrivacyRoute
import com.example.messenger.presentation.navigation.ProfileRoute
import com.example.messenger.presentation.navigation.SecurityRoute
import com.example.messenger.presentation.navigation.SettingsRoute
import com.example.messenger.presentation.screens.auth.AuthIntent
import com.example.messenger.presentation.screens.auth.AuthViewModel
import com.example.messenger.presentation.screens.call.CallScreen
import com.example.messenger.presentation.screens.calls.CallsScreen
import com.example.messenger.presentation.screens.changeaccount.ChangeAccountScreen
import com.example.messenger.presentation.screens.changephone.ChangePhoneScreen
import com.example.messenger.presentation.screens.chat.ChatScreenWithNav
import com.example.messenger.presentation.screens.chats.ChatsScreen
import com.example.messenger.presentation.screens.chatuserprofile.ChatUserProfileScreen
import com.example.messenger.presentation.screens.contacts.ContactsScreen
import com.example.messenger.presentation.screens.editchat.EditChatScreen
import com.example.messenger.presentation.screens.editcontactdata.EditContactDataScreen
import com.example.messenger.presentation.screens.editprofile.EditProfileScreen
import com.example.messenger.presentation.screens.profile.ProfileScreen
import com.example.messenger.presentation.screens.settings.NotificationsScreen
import com.example.messenger.presentation.screens.settings.PrivacyScreen
import com.example.messenger.presentation.screens.settings.SecurityScreen
import com.example.messenger.presentation.screens.settings.SettingsScreen
import com.example.messenger.presentation.screens.settings.appearance.AppearanceScreen
import com.example.messenger.presentation.screens.settings.datastorage.DataStorageScreen
import com.example.messenger.presentation.screens.settings.deleteaccount.DeleteAccountScreen
import com.example.messenger.presentation.screens.settings.language.LanguageScreen
import com.example.messenger.presentation.theme.LocalTabBarPosition
import com.example.messenger.presentation.theme.Motion
import com.example.messenger.presentation.theme.model.BarPosition

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun MainDisplay(
    authViewModel: AuthViewModel,
    pendingRoute: NavKey?,
    onRouteConsumed: () -> Unit,
) {
    val backStack = rememberNavBackStack(ChatsRoute)
    val context = LocalContext.current
    val tabViewModelOwner = remember(context) {
        var current: Context = context
        while (current is ContextWrapper) {
            if (current is ComponentActivity) break
            current = current.baseContext
        }
        current as? ViewModelStoreOwner
    }

    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            backStack.add(pendingRoute)
            onRouteConsumed()
        }
    }

    val currentRoute = backStack.lastOrNull()
    val isTabRoute =
        currentRoute is ChatsRoute || currentRoute is CallsRoute || currentRoute is SettingsRoute
    val showCallBar = currentRoute !is CallRoute && currentRoute !is AuthRoute
    val activeCall by ActiveCallHolder.state.collectAsStateWithLifecycle()
    val callBarMode by CallBarPresenter.mode.collectAsStateWithLifecycle()
    val floatingCallsEnabled = featureEnabled(FeatureFlag.CALLS_FLOATING_BUBBLE)
    LaunchedEffect(floatingCallsEnabled) { CallBarPresenter.setFloatingEnabled(floatingCallsEnabled) }
    val callBarVisible = activeCall?.let { isCallBarVisible(it) } == true
    val callBarDocked = callBarVisible && callBarMode == CallBarMode.BAR
    val incomingRinging = activeCall?.let { it.isIncoming && ! it.isActive } == true
    val callBarRoute = isTabRoute ||
            currentRoute is ChatRoute ||
            currentRoute is ChatUserProfileRoute ||
            currentRoute is EditChatRoute ||
            currentRoute is EditContactDataRoute ||
            currentRoute is ChangePhoneRoute
    val callBarInset by animateDpAsState(
        targetValue = if (callBarRoute && (callBarDocked || incomingRinging)) 84.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 300f),
        label = "callBarInset",
    )

    val clipPlayerViewModel: ClipPlayerViewModel =
        tabViewModelOwner?.let { hiltViewModel(it) } ?: hiltViewModel()
    val clipPlayback = clipPlayerViewModel.state.collectAsStateWithLifecycle()
    val clipPlaying by remember { derivedStateOf { clipPlayback.value.messageId != null } }
    val clipBarVisible = clipPlaying && callBarRoute && featureEnabled(FeatureFlag.CLIP_MINI_PLAYER)
    var clipBarHeightPx by remember { mutableStateOf(0) }
    val clipBarHeight = with(LocalDensity.current) { clipBarHeightPx.toDp() }
    val clipBarInset by animateDpAsState(
        targetValue = if (clipBarVisible && clipBarHeightPx > 0) {
            CLIP_BAR_TOP_OFFSET + clipBarHeight + CLIP_BAR_GAP
        } else {
            0.dp
        },
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 300f),
        label = "clipBarInset",
    )

    LaunchedEffect(activeCall == null) {
        if (activeCall == null) {
            CallBarPresenter.reset()
            VideoSurfaceOwner.reset()
        }
    }
    LaunchedEffect(currentRoute is CallRoute) {
        if (currentRoute is CallRoute) CallBarPresenter.showBar()
    }

    val callVideoOn = activeCall?.let { it.remoteVideoOn || it.localVideoOn } == true
    LaunchedEffect(callVideoOn, currentRoute is CallRoute, callBarMode) {
        if (currentRoute is CallRoute) return@LaunchedEffect
        when {
            callVideoOn && callBarMode == CallBarMode.BUBBLE -> CallBarPresenter.showVideoTile()
            ! callVideoOn && callBarMode == CallBarMode.VIDEO -> CallBarPresenter.collapseVideoTile()
        }
    }
    LaunchedEffect(activeCall?.remoteVideoOn, currentRoute is CallRoute) {
        if (activeCall?.remoteVideoOn == true && currentRoute !is CallRoute) {
            CallBarPresenter.showVideoTile()
        }
    }
    val selectedTab = when (currentRoute) {
        is CallsRoute -> MainTab.CALLS
        is SettingsRoute -> MainTab.SETTINGS
        else -> MainTab.CHATS
    }

    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchEnabled = featureEnabled(FeatureFlag.SEARCH)
    LaunchedEffect(isTabRoute, searchEnabled) {
        if (! isTabRoute || ! searchEnabled) {
            isSearching = false
            searchQuery = ""
        }
    }

    fun logout() {
        authViewModel.dispatch(AuthIntent.Logout)
    }

    fun navigateToTab(target: NavKey) {
        val tabRoots = listOf(ChatsRoute, CallsRoute, SettingsRoute)
        while (backStack.size > 1 && backStack.last() !in tabRoots) {
            backStack.removeLastOrNull()
        }
        if (backStack.lastOrNull() != target) {
            backStack.removeLastOrNull()
            backStack.add(target)
        }
    }

    fun popFromCall() {
        if (backStack.lastOrNull() is CallRoute) {
            val call = ActiveCallHolder.snapshot()
            if (call != null && ! call.callEnded && (call.remoteVideoOn || call.localVideoOn)) {
                CallBarPresenter.showVideoTile()
            }
        }
        backStack.removeLastOrNull()
    }

    fun openActiveCall(accept: Boolean) {
        val active = ActiveCallHolder.snapshot() ?: return
        if (accept) sendCallAction(context, CallForegroundService.ACTION_ACCEPT)
        if (backStack.lastOrNull() !is CallRoute) {
            backStack.add(
                CallRoute(
                    partnerId = active.callerId.ifBlank { "active" },
                    partnerName = active.partnerName.ifBlank { "Call" },
                    partnerPhone = active.partnerPhone.ifBlank { "_" },
                ),
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalOpenActiveCall provides { openActiveCall(accept = false) },
            LocalCallBarInset provides callBarInset,
            LocalClipBarInset provides clipBarInset,
        ) {
            SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalSharedTransitionScope provides this@SharedTransitionLayout) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavDisplay(
                            backStack = backStack,
                            onBack = { popFromCall() },
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator(),
                            ),
                            transitionSpec = {
                                (slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))) togetherWith
                                        (slideOutHorizontally(tween(300)) { - it } + fadeOut(
                                            tween(
                                                300
                                            )
                                        ))
                            },
                            popTransitionSpec = {
                                (slideInHorizontally(tween(300)) { - it } + fadeIn(tween(300))) togetherWith
                                        (slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)))
                            },
                            entryProvider = entryProvider {
                                entry<ChatsRoute> {
                                    ProvideNavAnimatedScope {
                                        ChatsScreen(
                                            viewModel = tabViewModelOwner?.let { hiltViewModel(it) }
                                                ?: hiltViewModel(),
                                            onChatClick = { conversationId, partnerId, partnerName, partnerAvatarUrl ->
                                                backStack.add(
                                                    ChatRoute(
                                                        conversationId,
                                                        partnerId,
                                                        partnerName,
                                                        partnerAvatarUrl
                                                    )
                                                )
                                            },
                                            onEditChats = { backStack.add(EditChatRoute) },
                                        )
                                    }
                                }

                                entry<CallsRoute> {
                                    CallsScreen(
                                        onCallBack = { partnerId, partnerName, video ->
                                            backStack.add(
                                                CallRoute(
                                                    partnerId,
                                                    partnerName,
                                                    "",
                                                    video = video
                                                )
                                            )
                                        },
                                        viewModel = tabViewModelOwner?.let { hiltViewModel(it) }
                                            ?: hiltViewModel(),
                                    )
                                }

                                entry<SettingsRoute> {
                                    SettingsScreen(
                                        viewModel = tabViewModelOwner?.let { hiltViewModel(it) }
                                            ?: hiltViewModel(),
                                        onProfileClick = { backStack.add(ProfileRoute) },
                                        onSwitchAccountClick = { backStack.add(ChangeAccountRoute) },
                                        onPrivacyClick = { backStack.add(PrivacyRoute) },
                                        onNotificationsClick = { backStack.add(NotificationsRoute) },
                                        onAppearanceClick = { backStack.add(AppearanceRoute) },
                                        onSecurityClick = { backStack.add(SecurityRoute) },
                                        onDataStorageClick = { backStack.add(DataStorageRoute) },
                                        onLanguageClick = { backStack.add(LanguageRoute) },
                                        onLogoutClick = { logout() },
                                    )
                                }

                                entry<PrivacyRoute> {
                                    PrivacyScreen(
                                        onBack = { backStack.removeLastOrNull() },
                                        onDeleteAccount = { backStack.add(DeleteAccountRoute) },
                                    )
                                }
                                entry<DeleteAccountRoute> {
                                    DeleteAccountScreen(
                                        onBackClick = { backStack.removeLastOrNull() },
                                        onDeleted = { backStack.removeLastOrNull() },
                                    )
                                }
                                entry<NotificationsRoute> { NotificationsScreen(onBack = { backStack.removeLastOrNull() }) }
                                entry<AppearanceRoute> { AppearanceScreen(onBack = { backStack.removeLastOrNull() }) }
                                entry<LanguageRoute> { LanguageScreen(onBack = { backStack.removeLastOrNull() }) }
                                entry<DataStorageRoute> { DataStorageScreen(onBack = { backStack.removeLastOrNull() }) }
                                entry<SecurityRoute> { SecurityScreen(onBack = { backStack.removeLastOrNull() }) }

                                entry<ProfileRoute> {
                                    ProfileScreen(
                                        onBackClick = { backStack.removeLastOrNull() },
                                        onStartEditing = { backStack.add(EditProfileRoute) },
                                    )
                                }

                                entry<EditProfileRoute> {
                                    EditProfileScreen(
                                        onBackClick = { backStack.removeLastOrNull() },
                                        onSaved = { backStack.removeLastOrNull() },
                                        onLogout = { logout() },
                                        onChangeAccount = { backStack.add(ChangeAccountRoute) },
                                        onChangePhone = { backStack.add(ChangePhoneRoute) },
                                    )
                                }

                                entry<ChangePhoneRoute> {
                                    ChangePhoneScreen(
                                        onBackClick = { backStack.removeLastOrNull() },
                                        onDone = { backStack.removeLastOrNull() },
                                    )
                                }

                                entry<ChangeAccountRoute> {
                                    ChangeAccountScreen(
                                        onBackClick = { backStack.removeLastOrNull() },
                                    )
                                }

                                entry<ContactsRoute> {
                                    ContactsScreen(
                                        onContactClick = { userId ->
                                            backStack.add(
                                                ChatUserProfileRoute(userId)
                                            )
                                        },
                                    )
                                }

                                entry<EditChatRoute> {
                                    EditChatScreen(onBackClick = { backStack.removeLastOrNull() })
                                }

                                entry<CallRoute> { key ->
                                    ProvideNavArgs(
                                        "partnerId" to key.partnerId,
                                        "partnerName" to key.partnerName,
                                        "partnerPhone" to key.partnerPhone,
                                        "video" to key.video,
                                    ) {
                                        CallScreen(
                                            onCallEnded = { backStack.removeLastOrNull() },
                                            onBack = { popFromCall() },
                                            onOpenChat = { conversationId, partnerId, partnerName ->
                                                backStack.removeLastOrNull()
                                                backStack.add(
                                                    ChatRoute(
                                                        conversationId,
                                                        partnerId,
                                                        partnerName
                                                    )
                                                )
                                            },
                                        )
                                    }
                                }

                                entry<ChatUserProfileRoute> { key ->
                                    ProvideNavArgs(
                                        "userId" to key.userId,
                                        "avatarUrl" to (key.avatarUrl ?: ""),
                                        "partnerName" to key.partnerName,
                                    ) {
                                        ProvideNavAnimatedScope {
                                            ChatUserProfileScreen(
                                                sharedKeyPartnerId = key.userId,
                                                onBackClick = { backStack.removeLastOrNull() },
                                                onCallClick = { partnerId, partnerName, partnerPhone ->
                                                    backStack.add(
                                                        CallRoute(
                                                            partnerId,
                                                            partnerName,
                                                            partnerPhone
                                                        )
                                                    )
                                                },
                                                onVideoCallClick = { partnerId, partnerName, partnerPhone ->
                                                    backStack.add(
                                                        CallRoute(
                                                            partnerId,
                                                            partnerName,
                                                            partnerPhone,
                                                            video = true
                                                        )
                                                    )
                                                },
                                                onEditClick = {
                                                    backStack.add(
                                                        EditContactDataRoute(
                                                            key.userId
                                                        )
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }

                                entry<EditContactDataRoute> { key ->
                                    ProvideNavArgs("contactId" to key.contactId) {
                                        EditContactDataScreen(onBackClick = { backStack.removeLastOrNull() })
                                    }
                                }

                                entry<ChatRoute> { key ->
                                    ProvideNavArgs(
                                        "conversationId" to key.conversationId,
                                        "partnerId" to key.partnerId,
                                        "partnerName" to key.partnerName,
                                        "partnerAvatarUrl" to (key.partnerAvatarUrl ?: ""),
                                    ) {
                                        ProvideNavAnimatedScope {
                                            ChatScreenWithNav(
                                                sharedKeyPartnerId = key.partnerId,
                                                onBackClick = { backStack.removeLastOrNull() },
                                                onIntercultorProfileClick = { avatarUrl, partnerName ->
                                                    if (key.partnerId.isNotBlank()) {
                                                        backStack.add(
                                                            ChatUserProfileRoute(
                                                                key.partnerId,
                                                                avatarUrl,
                                                                partnerName
                                                            )
                                                        )
                                                    }
                                                },
                                                onCallClick = {
                                                    backStack.add(
                                                        CallRoute(
                                                            key.partnerId,
                                                            key.partnerName,
                                                            ""
                                                        )
                                                    )
                                                },
                                                onVideoCallClick = {
                                                    backStack.add(
                                                        CallRoute(
                                                            key.partnerId,
                                                            key.partnerName,
                                                            "",
                                                            video = true
                                                        )
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }
                            },
                        )
                        if (isTabRoute && isSearching && searchEnabled) {
                            SearchResultsOverlay(
                                query = searchQuery,
                                onConversationCreated = { conversationId, partnerId, partnerName ->
                                    isSearching = false
                                    searchQuery = ""
                                    backStack.add(ChatRoute(conversationId, partnerId, partnerName))
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        val barAtBottom = LocalTabBarPosition.current == BarPosition.BOTTOM

                        AnimatedVisibility(
                            visible = isTabRoute,
                            enter = slideInVertically(
                                animationSpec = tween(
                                    durationMillis = Motion.durationMedium,
                                    delayMillis = Motion.durationMedium,
                                    easing = Motion.emphasized,
                                ),
                                initialOffsetY = { height -> if (barAtBottom) height else - height },
                            ) + fadeIn(
                                animationSpec = tween(
                                    durationMillis = Motion.durationShort,
                                    delayMillis = Motion.durationMedium,
                                ),
                            ),
                            exit = slideOutVertically(
                                animationSpec = tween(
                                    Motion.durationShort,
                                    easing = Motion.standard
                                ),
                                targetOffsetY = { height -> if (barAtBottom) height else - height },
                            ) + fadeOut(tween(Motion.durationShort)),
                            label = "FloatingTabBar",
                            modifier = Modifier
                                .align(if (barAtBottom) Alignment.BottomCenter else Alignment.TopCenter)
                                .then(
                                    if (barAtBottom) Modifier.navigationBarsPadding()
                                    else Modifier.statusBarsPadding(),
                                )
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                        ) {
                            FloatingTabBar(
                                selected = selectedTab,
                                onSelect = { tab -> navigateToTab(tab.toNavRoute()) },
                                isSearching = isSearching,
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                onSearchOpen = { isSearching = true },
                                onSearchClose = {
                                    isSearching = false
                                    searchQuery = ""
                                },
                            )
                        }
                    }
                }
            }
        }

        BackHandler(enabled = isTabRoute && isSearching) {
            isSearching = false
            searchQuery = ""
        }
        if (showCallBar) {
            IncomingCallBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 76.dp),
                onAccept = {
                    sendCallAction(context, CallForegroundService.ACTION_ACCEPT)
                    CallBarPresenter.showBar()
                },
                onDecline = { sendCallAction(context, CallForegroundService.ACTION_DECLINE) },
                onOpen = { openActiveCall(accept = false) },
            )
        }
        ActiveCallBar(
            onClick = { openActiveCall(accept = false) },
            routeAllowsBar = callBarRoute,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 68.dp),
        )
        AnimatedVisibility(
            visible = clipBarVisible,
            enter = fadeIn(tween(Motion.durationShort)),
            exit = fadeOut(tween(Motion.durationShort)),
            label = "ClipMiniPlayer",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = CLIP_BAR_TOP_OFFSET + callBarInset)
                .padding(horizontal = 16.dp)
                .onSizeChanged { clipBarHeightPx = it.height },
        ) {
            ClipMiniPlayer(
                playback = clipPlayback,
                onToggle = {
                    if (clipPlayback.value.isPlaying) clipPlayerViewModel.pause()
                    else clipPlayerViewModel.resume()
                },
                onCycleSpeed = {
                    clipPlayerViewModel.setSpeed(nextClipSpeed(clipPlayback.value.speed))
                },
                onClose = { clipPlayerViewModel.stop() },
                onOpen = {
                    val activeClipConversation = clipPlayback.value.conversationId
                    val target = backStack.lastOrNull()
                    val alreadyOpen = target is ChatRoute &&
                        target.conversationId == activeClipConversation
                    if (!alreadyOpen) {
                        activeClipConversation?.let { id ->
                            clipPlayerViewModel.resolveTarget(id) { resolved ->
                                backStack.add(
                                    ChatRoute(
                                        resolved.conversationId,
                                        resolved.partnerId,
                                        resolved.partnerName,
                                        resolved.partnerAvatarUrl,
                                    ),
                                )
                            }
                        }
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
        }
        if (currentRoute !is CallRoute && currentRoute !is AuthRoute) {
            CallBarOverlay(
                onOpenCall = { openActiveCall(accept = false) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun sendCallAction(context: Context, action: String) {
    val intent = Intent(context, CallForegroundService::class.java).setAction(action)
    runCatching { context.startService(intent) }
}

private fun MainTab.toNavRoute(): NavKey = when (this) {
    MainTab.CHATS -> ChatsRoute
    MainTab.CALLS -> CallsRoute
    MainTab.SETTINGS -> SettingsRoute
}

private val CLIP_BAR_TOP_OFFSET = 68.dp
private val CLIP_BAR_GAP = 8.dp
