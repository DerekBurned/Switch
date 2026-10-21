package com.example.messenger.presentation.screens.chatuserprofile

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.example.messenger.domain.model.User
import com.example.messenger.domain.usecase.user.GetUserByIdUseCase
import com.example.messenger.testutil.MainDispatcherExtension
import com.example.messenger.testutil.fakes.FakeUserRepository
import com.example.messenger.util.DELETED_USER_NAME
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ChatUserProfileViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val userRepository = FakeUserRepository()

    private fun createViewModel() = ChatUserProfileViewModel(
        getUserByIdUseCase = GetUserByIdUseCase(userRepository),
        userRepository = userRepository,
        savedStateHandle = SavedStateHandle(mapOf("userId" to "bob")),
    )

    @Test
    fun `contact alias overrides the username`() = runTest {
        userRepository.usersById["bob"] = User(id = "bob", username = "Bob")
        userRepository.contactAliases.value = mapOf("bob" to "Bobby")

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.displayName).isEqualTo("Bobby")
    }

    @Test
    fun `an online user is shown online`() = runTest {
        userRepository.usersById["bob"] = User(id = "bob", username = "Bob", isOnline = true)

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.isOnline).isTrue()
    }

    @Test
    fun `a deleted user is never shown online`() = runTest {
        userRepository.usersById["bob"] = User(id = "bob", username = "Bob", isOnline = true, deleted = true)
        userRepository.contactAliases.value = mapOf("bob" to "Bobby")

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.isOnline).isFalse()
        assertThat(viewModel.uiState.value.displayName).isEqualTo(DELETED_USER_NAME)
    }
}
