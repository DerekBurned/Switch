package com.example.messenger.presentation.screens.chatuserprofile

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.example.messenger.data.crypto.IdentityKeyStore
import com.example.messenger.data.crypto.KeyWrapper
import com.example.messenger.data.crypto.PeerKeyRegistry
import com.example.messenger.data.crypto.SafetyNumber
import com.example.messenger.data.crypto.model.PeerKey
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.testutil.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir
import java.io.File

private class PassThroughWrapper : KeyWrapper {
    override fun wrap(plain: ByteArray): ByteArray = plain
    override fun unwrap(wrapped: ByteArray): ByteArray = wrapped
}

class SafetyNumberViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    @TempDir
    lateinit var dir: File

    private val identityKeyStore by lazy { IdentityKeyStore(dir, PassThroughWrapper()) }
    private val peerKeyRegistry = mockk<PeerKeyRegistry>()
    private val authService = mockk<FirebaseAuthService> { every { getCurrentUserId() } returns "me" }
    private val peerPublicKey = ByteArray(32) { 7 }

    private fun createViewModel() = SafetyNumberViewModel(identityKeyStore, peerKeyRegistry, authService)

    @Test
    fun `missing peer key shows unavailable`() = runTest {
        coEvery { peerKeyRegistry.ensurePeerKey("bob") } returns null
        val viewModel = createViewModel()

        viewModel.load("bob")
        val state = viewModel.state.first { it.partnerId == "bob" }

        assertThat(state.available).isFalse()
        assertThat(state.digits).isEqualTo("")
    }

    @Test
    fun `known peer key shows digits and the verified flag`() = runTest {
        coEvery { peerKeyRegistry.ensurePeerKey("bob") } returns
            PeerKey(uid = "bob", publicKey = peerPublicKey, epoch = 1, verified = true)
        val viewModel = createViewModel()

        viewModel.load("bob")
        val state = viewModel.state.first { it.partnerId == "bob" }

        val myKey = identityKeyStore.getOrCreate("me").publicKey
        assertThat(state.available).isTrue()
        assertThat(state.verified).isTrue()
        assertThat(state.digits).isEqualTo(SafetyNumber.compute("me", myKey, "bob", peerPublicKey))
    }
}
