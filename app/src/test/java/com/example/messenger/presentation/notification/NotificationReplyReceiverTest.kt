package com.example.messenger.presentation.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.example.messenger.domain.featureflag.FeatureFlag
import com.example.messenger.domain.usecase.conversation.MarkConversationAsReadUseCase
import com.example.messenger.domain.usecase.message.SendMessageUseCase
import com.example.messenger.testutil.AndroidLogExtension
import com.example.messenger.testutil.fakes.FakeFeatureFlags
import dagger.hilt.android.EntryPointAccessors
import io.mockk.Called
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class NotificationReplyReceiverTest {

    @JvmField
    @RegisterExtension
    val log = AndroidLogExtension()

    private val featureFlags = FakeFeatureFlags()
    private val sendMessage = mockk<SendMessageUseCase>(relaxed = true)
    private val markRead = mockk<MarkConversationAsReadUseCase>(relaxed = true)
    private val entry = mockk<NotificationActionEntryPoint>()
    private val context = mockk<Context>(relaxed = true)
    private val pending = mockk<BroadcastReceiver.PendingResult>(relaxed = true)
    private val receiver = spyk(NotificationReplyReceiver())

    @BeforeEach
    fun setUp() {
        every { entry.featureFlags() } returns featureFlags
        every { entry.sendMessageUseCase() } returns sendMessage
        every { entry.markConversationAsReadUseCase() } returns markRead
        coEvery { markRead.invoke(any()) } returns Result.success(Unit)
        every { context.applicationContext } returns context
        every { receiver.goAsync() } returns pending
        mockkStatic(EntryPointAccessors::class)
        every {
            EntryPointAccessors.fromApplication(any<Context>(), NotificationActionEntryPoint::class.java)
        } returns entry
        mockkObject(ChatNotifier)
        every { ChatNotifier.refresh(any(), any()) } just Runs
        every { ChatNotifier.clear(any(), any()) } just Runs
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(EntryPointAccessors::class)
        unmockkObject(ChatNotifier)
    }

    private fun intent(action: String): Intent = mockk {
        every { this@mockk.action } returns action
        every { getStringExtra(any()) } returns null
        every { getStringExtra(NotificationReplyReceiver.EXTRA_CONVERSATION_ID) } returns "conv1"
    }

    @Test
    fun `a switched-off reply sends nothing and refreshes the notification`() {
        featureFlags.setOff(FeatureFlag.NOTIFICATION_INLINE_REPLY)

        receiver.onReceive(context, intent(NotificationReplyReceiver.ACTION_REPLY))

        verify(timeout = 5_000) { pending.finish() }
        verify { ChatNotifier.refresh(context, "conv1") }
        verify { sendMessage wasNot Called }
    }

    @Test
    fun `a switched-off mark-read leaves the conversation unread and refreshes the notification`() {
        featureFlags.setOff(FeatureFlag.NOTIFICATION_MARK_READ)

        receiver.onReceive(context, intent(NotificationReplyReceiver.ACTION_MARK_READ))

        verify(timeout = 5_000) { pending.finish() }
        verify { ChatNotifier.refresh(context, "conv1") }
        verify { markRead wasNot Called }
    }

    @Test
    fun `an enabled mark-read marks the conversation and clears the notification`() {
        receiver.onReceive(context, intent(NotificationReplyReceiver.ACTION_MARK_READ))

        verify(timeout = 5_000) { pending.finish() }
        coVerify { markRead.invoke("conv1") }
        verify { ChatNotifier.clear(context, "conv1") }
    }

    @Test
    fun `flags are refreshed before the action is decided`() {
        receiver.onReceive(context, intent(NotificationReplyReceiver.ACTION_MARK_READ))

        verify(timeout = 5_000) { pending.finish() }
        assertThat(featureFlags.awaitFreshCalls).isEqualTo(1)
    }
}
