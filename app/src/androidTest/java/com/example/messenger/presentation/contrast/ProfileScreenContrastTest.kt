package com.example.messenger.presentation.contrast

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.messenger.domain.model.PhoneNumber
import com.example.messenger.domain.model.User
import com.example.messenger.presentation.components.profile.MediaTabsRow
import com.example.messenger.presentation.components.profile.ProfileInfoCard
import com.example.messenger.presentation.screens.chatuserprofile.ChatUserProfileUiState
import com.example.messenger.presentation.screens.chatuserprofile.MediaTab
import com.example.messenger.presentation.screens.editprofile.EditProfileScreenContent
import com.example.messenger.presentation.screens.editprofile.EditProfileUiState
import com.example.messenger.presentation.screens.profile.ProfileScreenContent
import com.example.messenger.presentation.screens.profile.ProfileUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenContrastTest {

    private val profileUser = User(
        id = "u1",
        username = "alice",
        email = "alice@example.com",
        phoneNumber = PhoneNumber(countryCode = "+1", number = "5550100"),
    )

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun profileScreenRows() = compose.assertReadableInBothThemes(
        component = "ProfileScreenContent",
        texts = setOf(
            "Phone number",
            "+15550100",
            "Name",
            "alice",
            "Username",
            "@alice",
            "Email",
            "alice@example.com",
        ),
    ) {
        ProfileScreenContent(
            uiState = ProfileUiState(user = profileUser),
            onBackClick = {},
            onEditClick = {},
        )
    }

    @Test
    fun chatUserProfileInfoCardRows() = compose.assertReadableInBothThemes("ProfileInfoCard") {
        ProfileInfoCard(state = ChatUserProfileUiState(user = profileUser))
    }

    @Test
    fun chatUserProfileInactiveMediaTabs() = compose.assertReadableInBothThemes(
        component = "MediaTabsRow",
        texts = setOf("Video", "File"),
    ) {
        MediaTabsRow(active = MediaTab.PHOTO, onChange = {})
    }

    @Test
    fun editProfilePhoneRow() = compose.assertReadableInBothThemes(
        component = "EditProfileScreenContent",
        texts = setOf("Phone number", "+15550100"),
    ) {
        EditProfileScreenContent(
            state = EditProfileUiState(
                name = "Alice Johnson",
                phone = "+15550100",
                username = "alice",
            ),
        )
    }
}
