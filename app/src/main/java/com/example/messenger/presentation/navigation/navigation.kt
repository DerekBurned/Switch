package com.example.messenger.presentation.navigation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.example.messenger.data.remote.call.ActiveCallHolder
import com.example.messenger.data.remote.call.CallForegroundService
import com.example.messenger.presentation.components.call.ActiveCallBar
import com.example.messenger.presentation.components.call.CallBarMode
import com.example.messenger.presentation.components.call.CallBarOverlay
import com.example.messenger.presentation.components.call.CallBarPresenter
import com.example.messenger.presentation.components.call.IncomingCallBar
import com.example.messenger.presentation.components.call.LocalCallBarInset
import com.example.messenger.presentation.components.call.LocalOpenActiveCall
import com.example.messenger.presentation.components.call.isCallBarVisible
import com.example.messenger.presentation.components.common.FloatingTabBar
import com.example.messenger.presentation.components.common.LocalNavAnimatedVisibilityScope
import com.example.messenger.presentation.components.common.LocalSharedTransitionScope
import com.example.messenger.presentation.components.common.MainTab
import com.example.messenger.presentation.components.common.WallpaperBackdrop
import com.example.messenger.presentation.components.search.SearchResultsOverlay
import com.example.messenger.presentation.intent.AuthIntent
import com.example.messenger.presentation.notification.NotificationPermissionGate
import com.example.messenger.presentation.screens.*
import com.example.messenger.presentation.screens.settings.AppearanceScreen
import com.example.messenger.presentation.screens.settings.DataStorageScreen
import com.example.messenger.presentation.screens.settings.LanguageScreen
import com.example.messenger.presentation.screens.settings.NotificationsScreen
import com.example.messenger.presentation.screens.settings.PrivacyScreen
import com.example.messenger.presentation.screens.settings.SecurityScreen
import com.example.messenger.presentation.screens.ui.theme.BarPosition
import com.example.messenger.presentation.screens.ui.theme.LocalTabBarPosition
import com.example.messenger.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(
    pendingRoute: NavKey? = null,
    onRouteConsumed: () -> Unit = {},
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Single static wallpaper for the whole app: screens are transparent,
        // so navigation transitions animate only their content over it.
        WallpaperBackdrop()
        if (authState.isAuthenticated) {
            MainDisplay(
                authViewModel = authViewModel,
                pendingRoute = pendingRoute,
                onRouteConsumed = onRouteConsumed,
            )
        } else {
            AuthDisplay(authViewModel = authViewModel)
        }
        NotificationPermissionGate(isAuthenticated = authState.isAuthenticated)
    }
}

@Composable
private fun AuthDisplay(authViewModel: AuthViewModel) {
    val backStack = rememberNavBackStack(AuthRoute)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AuthRoute> {
                AuthScreen(
                    viewModel = authViewModel,
                    onAuthSuccess = {},
                )
            }
        },
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MainDisplay(
    authViewModel: AuthViewModel,
    pendingRoute: NavKey?,
    onRouteConsumed: () -> Unit,
) {
    val backStack = rememberNavBackStack(ChatsRoute)
    val context = LocalContext.current
    val tabViewModelOwner = remember(context) {
        var current: Context = context
        while (current is android.content.ContextWrapper) {
            if (current is androidx.activity.ComponentActivity) break
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
    val isTabRoute = currentRoute is ChatsRoute || currentRoute is CallsRoute || currentRoute is SettingsRoute
    val showCallBar = currentRoute !is CallRoute && currentRoute !is AuthRoute
    val activeCall by ActiveCallHolder.state.collectAsStateWithLifecycle()
    val callBarMode by CallBarPresenter.mode.collectAsStateWithLifecycle()
    val callBarVisible = activeCall?.let { isCallBarVisible(it) } == true
    val callBarDocked = callBarVisible && callBarMode == CallBarMode.BAR
    val incomingRinging = activeCall?.let { it.isIncoming && !it.isActive } == true
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

    LaunchedEffect(activeCall == null) {
        if (activeCall == null) CallBarPresenter.reset()
    }
    LaunchedEffect(currentRoute is CallRoute) {
        if (currentRoute is CallRoute) CallBarPresenter.showBar()
    }
    val selectedTab = when (currentRoute) {
        is CallsRoute -> MainTab.CALLS
        is SettingsRoute -> MainTab.SETTINGS
        else -> MainTab.CHATS
    }

    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    LaunchedEffect(isTabRoute) {
        if (!isTabRoute) {
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
        ) {
            SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalSharedTransitionScope provides this@SharedTransitionLayout) {
                    Box(modifier = Modifier.fillMaxSize()) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        transitionSpec = {
                            (slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))) togetherWith
                                (slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)))
                        },
                        popTransitionSpec = {
                            (slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300))) togetherWith
                                (slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)))
                        },
                        entryProvider = entryProvider {
                            entry<ChatsRoute> {
                                ProvideNavAnimatedScope {
                                    ChatsScreen(
                                        viewModel = tabViewModelOwner?.let { hiltViewModel(it) } ?: hiltViewModel(),
                                        onChatClick = { conversationId, partnerId, partnerName, partnerAvatarUrl ->
                                            backStack.add(ChatRoute(conversationId, partnerId, partnerName, partnerAvatarUrl))
                                        },
                                        onEditChats = { backStack.add(EditChatRoute) },
                                        onLogoutClick = { logout() },
                                    )
                                }
                            }

                            entry<CallsRoute> {
                                CallsScreen(
                                    onLogoutClick = { logout() },
                                    onCallBack = { partnerId, partnerName, video ->
                                        backStack.add(CallRoute(partnerId, partnerName, "", video = video))
                                    },
                                    viewModel = tabViewModelOwner?.let { hiltViewModel(it) } ?: hiltViewModel(),
                                )
                            }

                            entry<SettingsRoute> {
                                SettingsScreen(
                                    viewModel = tabViewModelOwner?.let { hiltViewModel(it) } ?: hiltViewModel(),
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

                            entry<PrivacyRoute> { PrivacyScreen(onBack = { backStack.removeLastOrNull() }) }
                            entry<NotificationsRoute> { NotificationsScreen(onBack = { backStack.removeLastOrNull() }) }
                            entry<AppearanceRoute> { AppearanceScreen(onBack = { backStack.removeLastOrNull() }) }
                            entry<LanguageRoute> { LanguageScreen(onBack = { backStack.removeLastOrNull() }) }
                            entry<DataStorageRoute> { DataStorageScreen(onBack = { backStack.removeLastOrNull() }) }
                            entry<SecurityRoute> { SecurityScreen(onBack = { backStack.removeLastOrNull() }) }

                            entry<ProfileRoute> {
                                ProfileScreen(
                                    onBackClick = { backStack.removeLastOrNull() },
                                    onLogoutClick = { logout() },
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
                                    onAddAccount = { backStack.add(AuthRoute) },
                                )
                            }

                            entry<ContactsRoute> {
                                ContactsScreen(
                                    onLogoutClick = { logout() },
                                    onContactClick = { userId -> backStack.add(ChatUserProfileRoute(userId)) },
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
                                        onBack = { backStack.removeLastOrNull() },
                                        onOpenChat = { conversationId, partnerId, partnerName ->
                                            backStack.removeLastOrNull()
                                            backStack.add(ChatRoute(conversationId, partnerId, partnerName))
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
                                                backStack.add(CallRoute(partnerId, partnerName, partnerPhone))
                                            },
                                            onVideoCallClick = { partnerId, partnerName, partnerPhone ->
                                                backStack.add(CallRoute(partnerId, partnerName, partnerPhone, video = true))
                                            },
                                            onEditClick = { backStack.add(EditContactDataRoute(key.userId)) },
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
                                                    backStack.add(ChatUserProfileRoute(key.partnerId, avatarUrl, partnerName))
                                                }
                                            },
                                            onCallClick = {
                                                backStack.add(CallRoute(key.partnerId, key.partnerName, ""))
                                            },
                                            onVideoCallClick = {
                                                backStack.add(CallRoute(key.partnerId, key.partnerName, "", video = true))
                                            },
                                        )
                                    }
                                }
                            }
                        },
                    )
                    if (isTabRoute && isSearching) {
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
                    if (isTabRoute) {
                        val barAtBottom = LocalTabBarPosition.current == BarPosition.BOTTOM
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
                            modifier = Modifier
                                .align(if (barAtBottom) Alignment.BottomCenter else Alignment.TopCenter)
                                .then(
                                    if (barAtBottom) Modifier.navigationBarsPadding()
                                    else Modifier.statusBarsPadding(),
                                )
                                .padding(horizontal = 20.dp, vertical = 8.dp),
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
        if (currentRoute !is CallRoute && currentRoute !is AuthRoute) {
            CallBarOverlay(
                onOpenCall = { openActiveCall(accept = false) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ProvideNavAnimatedScope(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNavAnimatedVisibilityScope provides LocalNavAnimatedContentScope.current,
    ) {
        content()
    }
}

@Composable
internal fun ProvideNavArgs(vararg args: Pair<String, Any?>, content: @Composable () -> Unit) {
    val owner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }
    val entries = args.toList()
    val wrapped = remember(owner, entries) {
        NavArgsViewModelStoreOwner(owner, bundleOf(*entries.toTypedArray()))
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides wrapped) {
        content()
    }
}

private class NavArgsViewModelStoreOwner(
    private val delegate: ViewModelStoreOwner,
    private val args: Bundle,
) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {

    private val base = delegate as HasDefaultViewModelProviderFactory

    override val viewModelStore: ViewModelStore
        get() = delegate.viewModelStore

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = base.defaultViewModelProviderFactory

    override val defaultViewModelCreationExtras: CreationExtras
        get() = MutableCreationExtras(base.defaultViewModelCreationExtras).apply {
            this[DEFAULT_ARGS_KEY] = args
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
