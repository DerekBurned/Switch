package com.example.messenger.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object AuthRoute : NavKey

@Serializable
data object ChatsRoute : NavKey

@Serializable
data object CallsRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data object ContactsRoute : NavKey

@Serializable
data object ProfileRoute : NavKey

@Serializable
data object EditProfileRoute : NavKey

@Serializable
data object ChangeAccountRoute : NavKey

@Serializable
data object ChangePhoneRoute : NavKey

@Serializable
data object EditChatRoute : NavKey

@Serializable
data object PrivacyRoute : NavKey

@Serializable
data object NotificationsRoute : NavKey

@Serializable
data object AppearanceRoute : NavKey

@Serializable
data object LanguageRoute : NavKey

@Serializable
data object DataStorageRoute : NavKey

@Serializable
data object SecurityRoute : NavKey

@Serializable
data class ChatRoute(
    val conversationId: String,
    val partnerId: String = "",
    val partnerName: String = "",
    val partnerAvatarUrl: String? = null,
) : NavKey

@Serializable
data class ChatUserProfileRoute(
    val userId: String,
    val avatarUrl: String? = null,
    val partnerName: String = "",
) : NavKey

@Serializable
data class EditContactDataRoute(val contactId: String) : NavKey

@Serializable
data class CallRoute(
    val partnerId: String,
    val partnerName: String,
    val partnerPhone: String,
    val video: Boolean = false,
) : NavKey
