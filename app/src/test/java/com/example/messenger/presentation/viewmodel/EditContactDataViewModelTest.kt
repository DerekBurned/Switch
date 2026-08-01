package com.example.messenger.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.example.messenger.domain.usecase.user.GetUserByIdUseCase
import com.example.messenger.presentation.effect.EditContactDataEffect
import com.example.messenger.testutil.MainDispatcherExtension
import com.example.messenger.testutil.fakes.FakeUserRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class EditContactDataViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val userRepository = FakeUserRepository()

    private fun createViewModel(contactId: String?) = EditContactDataViewModel(
        getUserByIdUseCase = GetUserByIdUseCase(userRepository),
        userRepository = userRepository,
        savedStateHandle = SavedStateHandle(mapOf("contactId" to contactId)),
    )

    @Test
    fun `existing alias is loaded into the editable name`() = runTest {
        userRepository.contactAliases.value = mapOf("c1" to "Bob")

        val viewModel = createViewModel("c1")

        assertThat(viewModel.state.value.name).isEqualTo("Bob")
        assertThat(viewModel.state.value.initialName).isEqualTo("Bob")
    }

    @Test
    fun `saving a blank name sets an error and does not touch the repository`() = runTest {
        userRepository.contactAliases.value = mapOf("c1" to "Bob")
        val viewModel = createViewModel("c1")

        viewModel.onNameChange("")
        viewModel.save()

        assertThat(viewModel.state.value.error).isNotNull()
        assertThat(userRepository.updatedContactNames).isEmpty()
    }

    @Test
    fun `saving a valid name persists it and emits Saved`() = runTest {
        userRepository.contactAliases.value = mapOf("c1" to "Bob")
        val viewModel = createViewModel("c1")
        viewModel.onNameChange("Robert")

        viewModel.effect.test {
            viewModel.save()
            assertThat(awaitItem()).isEqualTo(EditContactDataEffect.Saved)
        }

        assertThat(userRepository.updatedContactNames).containsExactly("c1" to "Robert")
        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.initialName).isEqualTo("Robert")
    }

    @Test
    fun `delete emits the Deleted effect`() = runTest {
        val viewModel = createViewModel("c1")

        viewModel.effect.test {
            viewModel.delete()
            assertThat(awaitItem()).isEqualTo(EditContactDataEffect.Deleted)
        }
    }
}
