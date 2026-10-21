package com.example.messenger.presentation.screens.forwardmessage

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import com.example.messenger.domain.usecase.conversation.GetConversationsUseCase
import com.example.messenger.presentation.base.UiText
import com.example.messenger.testutil.MainDispatcherExtension
import com.example.messenger.testutil.fakes.FakeConversationRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ForwardMessageViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val conversationRepository = FakeConversationRepository()

    private fun createViewModel() = ForwardMessageViewModel(GetConversationsUseCase(conversationRepository))

    @Test
    fun `closing search clears the query`() = runTest {
        val viewModel = createViewModel()
        viewModel.setSearchActive(true)
        viewModel.onQueryChange("bo")

        viewModel.setSearchActive(false)

        assertThat(viewModel.uiState.value.query).isEqualTo("")
        assertThat(viewModel.uiState.value.searchActive).isFalse()
    }

    @Test
    fun `opening search keeps the typed query`() = runTest {
        val viewModel = createViewModel()
        viewModel.onQueryChange("bo")

        viewModel.setSearchActive(true)

        assertThat(viewModel.uiState.value.query).isEqualTo("bo")
    }

    @Test
    fun `list error is shown and loading clears`() = runTest {
        conversationRepository.conversationsError = IllegalStateException("offline")

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.error).isEqualTo(UiText.DynamicString("offline"))
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }
}
