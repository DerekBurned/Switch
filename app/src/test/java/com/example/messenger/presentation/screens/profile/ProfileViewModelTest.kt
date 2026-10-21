package com.example.messenger.presentation.screens.profile

import android.net.Uri
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import com.example.messenger.R
import com.example.messenger.domain.model.User
import com.example.messenger.domain.usecase.auth.GetCurrentUserUseCase
import com.example.messenger.domain.usecase.user.GetUserByIdUseCase
import com.example.messenger.presentation.base.UiText
import com.example.messenger.testutil.MainDispatcherExtension
import com.example.messenger.testutil.fakes.FakeAuthRepository
import com.example.messenger.testutil.fakes.FakeUserRepository
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ProfileViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val authRepository = FakeAuthRepository().apply {
        loggedInUser = User(id = "me", username = "auth name")
    }
    private val userRepository = FakeUserRepository()

    private fun createViewModel() = ProfileViewModel(
        getCurrentUserUseCase = GetCurrentUserUseCase(authRepository),
        getUserByIdUseCase = GetUserByIdUseCase(userRepository),
        userRepository = userRepository,
    )

    @Test
    fun `no signed-in user shows no profile`() = runTest {
        authRepository.loggedInUser = null

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.user).isNull()
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `remote profile replaces the auth user`() = runTest {
        userRepository.usersById["me"] = User(id = "me", username = "remote name")

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.user?.username).isEqualTo("remote name")
    }

    @Test
    fun `a missing remote profile keeps the auth user`() = runTest {
        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.user?.username).isEqualTo("auth name")
    }

    @Test
    fun `a blank username is rejected without saving`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateProfile("   ")

        assertThat((viewModel.uiState.value.error as UiText.StringResource).id)
            .isEqualTo(R.string.profile_error_username_empty)
        assertThat(userRepository.lastProfileUpdate).isNull()
    }

    @Test
    fun `avatar upload updates the avatar and reloads photos`() = runTest {
        val viewModel = createViewModel()
        userRepository.uploadedAvatarUrl = "new.png"
        userRepository.profilePhotos = listOf("new.png", "old.png")

        viewModel.addPhoto(mockk<Uri>())

        assertThat(viewModel.uiState.value.user?.avatarUrl).isEqualTo("new.png")
        assertThat(viewModel.uiState.value.photos).containsExactly("new.png", "old.png")
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }
}
