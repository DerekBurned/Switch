package com.example.messenger.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.messenger.domain.featureflag.FeatureFlags
import com.example.messenger.presentation.components.common.WallpaperBackground
import com.example.messenger.presentation.featureflag.LocalFeatureFlags
import com.example.messenger.presentation.navigation.ChatRoute
import com.example.messenger.presentation.navigation.graph.ProvideNavArgs
import com.example.messenger.presentation.screens.chat.ChatScreenWithNav
import com.example.messenger.presentation.theme.MessengerTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BubbleActivity : ComponentActivity() {

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var featureFlags: FeatureFlags

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        if (firebaseAuth.currentUser == null) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (auth.currentUser == null) {
            finish()
            return
        }
        auth.addAuthStateListener(authListener)
        val conversationId = intent.getStringExtra(MainActivity.EXTRA_OPEN_CONVERSATION_ID).orEmpty()
        val partnerId = intent.getStringExtra(MainActivity.EXTRA_PARTNER_ID).orEmpty()
        val partnerName = intent.getStringExtra(MainActivity.EXTRA_PARTNER_NAME).orEmpty()
        if (conversationId.isBlank()) {
            finish()
            return
        }
        setContent {
            val flagState by featureFlags.state.collectAsStateWithLifecycle()
            MessengerTheme {
                val backStack = rememberNavBackStack(
                    ChatRoute(conversationId, partnerId, partnerName),
                )
                CompositionLocalProvider(LocalFeatureFlags provides flagState) {
                    WallpaperBackground {
                        NavDisplay(
                            backStack = backStack,
                            onBack = { finish() },
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator(),
                            ),
                            entryProvider = entryProvider {
                                entry<ChatRoute> { key ->
                                    ProvideNavArgs(
                                        "conversationId" to key.conversationId,
                                        "partnerId" to key.partnerId,
                                        "partnerName" to key.partnerName,
                                    ) {
                                        ChatScreenWithNav(
                                            inBubble = true,
                                            onBackClick = { finish() },
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        auth.removeAuthStateListener(authListener)
        super.onDestroy()
    }
}
