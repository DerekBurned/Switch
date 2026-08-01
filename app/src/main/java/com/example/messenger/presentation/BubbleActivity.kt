package com.example.messenger.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.messenger.presentation.components.common.WallpaperBackground
import com.example.messenger.presentation.navigation.ChatRoute
import com.example.messenger.presentation.navigation.ProvideNavArgs
import com.example.messenger.presentation.screens.ChatScreenWithNav
import com.example.messenger.presentation.screens.ui.theme.MessengerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BubbleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val conversationId = intent.getStringExtra(MainActivity.EXTRA_OPEN_CONVERSATION_ID).orEmpty()
        val partnerId = intent.getStringExtra(MainActivity.EXTRA_PARTNER_ID).orEmpty()
        val partnerName = intent.getStringExtra(MainActivity.EXTRA_PARTNER_NAME).orEmpty()
        if (conversationId.isBlank()) {
            finish()
            return
        }
        setContent {
            MessengerTheme {
                val backStack = rememberNavBackStack(
                    ChatRoute(conversationId, partnerId, partnerName),
                )
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
