package com.example.messenger.presentation.screens.editchat

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.example.messenger.domain.model.Conversation
import com.example.messenger.domain.usecase.conversation.DeleteConversationUseCase
import com.example.messenger.domain.usecase.conversation.MarkConversationAsReadUseCase
import com.example.messenger.domain.usecase.conversation.ObserveConversationsUseCase
import com.example.messenger.testutil.MainDispatcherExtension
import com.example.messenger.testutil.fakes.FakeConversationRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class EditChatViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val conversationRepository = FakeConversationRepository()

    private fun createViewModel() = EditChatViewModel(
        observeConversationsUseCase = ObserveConversationsUseCase(conversationRepository),
        deleteConversationUseCase = DeleteConversationUseCase(conversationRepository),
        markConversationAsReadUseCase = MarkConversationAsReadUseCase(conversationRepository),
    )

    private fun conversations(vararg ids: String, preview: String = "hi") =
        ids.map { Conversation(id = it, lastMessage = preview) }

    @Test
    fun `all conversations start selected`() = runTest {
        conversationRepository.conversations.value = conversations("a", "b")

        val viewModel = createViewModel()

        assertThat(viewModel.state.value.selectedIds).containsExactlyInAnyOrder("a", "b")
    }

    @Test
    fun `chats that arrive after an empty first list start selected`() = runTest {
        val viewModel = createViewModel()

        conversationRepository.conversations.value = conversations("a", "b")

        assertThat(viewModel.state.value.selectedIds).containsExactlyInAnyOrder("a", "b")
    }

    @Test
    fun `deselecting every chat survives a list update`() = runTest {
        conversationRepository.conversations.value = conversations("a", "b")
        val viewModel = createViewModel()
        viewModel.toggle("a")
        viewModel.toggle("b")

        conversationRepository.conversations.value = conversations("a", "b", preview = "new message")

        assertThat(viewModel.state.value.selectedIds).isEmpty()
    }

    @Test
    fun `deleteSelected deletes only the selected ids, then emits Done`() = runTest {
        conversationRepository.conversations.value = conversations("a", "b", "c")
        val viewModel = createViewModel()
        viewModel.toggle("b")

        viewModel.effect.test {
            viewModel.deleteSelected()

            assertThat(awaitItem()).isEqualTo(EditChatEffect.Done)
        }
        assertThat(conversationRepository.deletedIds).containsExactly("a", "c")
    }
}
