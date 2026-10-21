package com.example.messenger.presentation.screens.editprofile

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.example.messenger.R
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.domain.model.User
import com.example.messenger.presentation.base.UiText
import com.example.messenger.testutil.MainDispatcherExtension
import com.example.messenger.testutil.fakes.FakeUserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class EditProfileViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val userRepository = FakeUserRepository()
    private val authService = mockk<FirebaseAuthService> {
        every { getCurrentUserId() } returns "me"
        every { getUserPhoneNumber() } returns "+12025550123"
        coEvery { updateProfile(any(), any()) } returns Result.success(Unit)
    }

    private fun createViewModel(saved: Map<String, Any?> = emptyMap()) = EditProfileViewModel(
        userRepository = userRepository,
        firebaseAuthService = authService,
        savedStateHandle = SavedStateHandle(saved),
    )

    @Test
    fun `profile loads once and is not reloaded after process death`() = runTest {
        val viewModel = createViewModel(
            mapOf("edit_profile_loaded" to true, "edit_profile_username" to "kept"),
        )

        assertThat(viewModel.state.value.username).isEqualTo("kept")
        assertThat(userRepository.requestedUserIds).isEmpty()
    }

    @Test
    fun `phone falls back to the auth number when the profile has none`() = runTest {
        userRepository.usersById["me"] = User(id = "me", username = "bob")

        val viewModel = createViewModel()

        assertThat(viewModel.state.value.username).isEqualTo("bob")
        assertThat(viewModel.state.value.phone).isEqualTo("+12025550123")
    }

    @Test
    fun `a blank username shows an error`() = runTest {
        val viewModel = createViewModel()
        viewModel.dispatch(EditProfileIntent.UsernameChange(""))

        viewModel.dispatch(EditProfileIntent.Save)

        assertThat((viewModel.state.value.error as UiText.StringResource).id)
            .isEqualTo(R.string.edit_profile_error_username_empty)
        assertThat(userRepository.lastProfileUpdate).isNull()
    }

    @Test
    fun `save writes username and usernameLower, then emits Saved`() = runTest {
        val viewModel = createViewModel()
        viewModel.dispatch(EditProfileIntent.UsernameChange("Bob"))

        viewModel.effect.test {
            viewModel.dispatch(EditProfileIntent.Save)

            assertThat(awaitItem()).isEqualTo(EditProfileEffect.Saved)
        }
        assertThat(userRepository.lastProfileUpdate)
            .isEqualTo(mapOf<String, Any>("username" to "Bob", "usernameLower" to "bob"))
        coVerify { authService.updateProfile(displayName = "Bob", photoUrl = null) }
    }
}
