package com.example.messenger.presentation.screens.settings.deleteaccount

import android.app.Activity
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.example.messenger.R
import com.example.messenger.data.presence.PresenceManager
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.domain.usecase.auth.DeleteAccountUseCase
import com.example.messenger.presentation.base.UiText
import com.example.messenger.testutil.MainDispatcherExtension
import com.example.messenger.testutil.fakes.FakeAuthRepository
import com.example.messenger.testutil.fakes.FakeLocalRepository
import com.example.messenger.testutil.fakes.FakeSessionCleaner
import com.example.messenger.util.VerificationResult
import com.google.firebase.auth.PhoneAuthCredential
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class DeleteAccountViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val authRepository = FakeAuthRepository()
    private val authService = mockk<FirebaseAuthService> {
        every { getUserPhoneNumber() } returns "+12025550123"
    }
    private val presenceManager = mockk<PresenceManager>(relaxed = true)
    private val activity = mockk<Activity>()
    private val credential = mockk<PhoneAuthCredential>()

    private fun createViewModel() = DeleteAccountViewModel(
        firebaseAuthService = authService,
        deleteAccountUseCase = DeleteAccountUseCase(authRepository, FakeLocalRepository(), FakeSessionCleaner()),
        presenceManager = presenceManager,
    )

    private fun DeleteAccountUiState.errorId(): Int? = (error as? UiText.StringResource)?.id

    private fun DeleteAccountViewModel.reachVerifyStep() {
        coEvery { authService.sendVerificationCode("+12025550123", activity) } returns
            Result.success(VerificationResult.CodeSent("vid"))
        dispatch(DeleteAccountIntent.ConfirmationChange("DELETE"))
        dispatch(DeleteAccountIntent.SendVerificationCode(activity))
        dispatch(DeleteAccountIntent.OtpChange("123456"))
        every { authService.verifyCode("123456", "vid") } returns Result.success(credential)
    }

    @Test
    fun `sending the code before typing the confirmation shows an error and calls nothing`() = runTest {
        val viewModel = createViewModel()

        viewModel.dispatch(DeleteAccountIntent.SendVerificationCode(activity))

        assertThat(viewModel.state.value.errorId()).isEqualTo(R.string.delete_account_error_confirm)
        coVerify(exactly = 0) { authService.sendVerificationCode(any(), any()) }
    }

    @Test
    fun `no phone number on file shows the no-number error`() = runTest {
        every { authService.getUserPhoneNumber() } returns null
        val viewModel = createViewModel()
        viewModel.dispatch(DeleteAccountIntent.ConfirmationChange("DELETE"))

        viewModel.dispatch(DeleteAccountIntent.SendVerificationCode(activity))

        assertThat(viewModel.state.value.errorId()).isEqualTo(R.string.delete_account_error_no_number)
    }

    @Test
    fun `code sent moves to the verify step`() = runTest {
        val viewModel = createViewModel()

        viewModel.reachVerifyStep()

        assertThat(viewModel.state.value.step).isEqualTo(DeleteAccountStep.VERIFY)
        assertThat(viewModel.state.value.verificationId).isEqualTo("vid")
    }

    @Test
    fun `failed re-authentication does not delete the account`() = runTest {
        val viewModel = createViewModel()
        viewModel.reachVerifyStep()
        coEvery { authService.reauthenticateWithPhone(credential) } returns Result.failure(Exception())

        viewModel.dispatch(DeleteAccountIntent.VerifyOtpAndDelete)

        assertThat(authRepository.deleteCalls).isEqualTo(0)
        assertThat(viewModel.state.value.errorId()).isEqualTo(R.string.delete_account_error_reauth)
    }

    @Test
    fun `successful deletion emits Deleted`() = runTest {
        val viewModel = createViewModel()
        viewModel.reachVerifyStep()
        coEvery { authService.reauthenticateWithPhone(credential) } returns Result.success(Unit)

        viewModel.effect.test {
            viewModel.dispatch(DeleteAccountIntent.VerifyOtpAndDelete)

            assertThat(awaitItem()).isEqualTo(DeleteAccountEffect.Deleted)
        }
        assertThat(authRepository.deleteCalls).isEqualTo(1)
        verify { presenceManager.disconnect(any()) }
    }

    @Test
    fun `go back returns to confirm and clears the code`() = runTest {
        val viewModel = createViewModel()
        viewModel.reachVerifyStep()

        viewModel.dispatch(DeleteAccountIntent.GoBack)

        assertThat(viewModel.state.value.step).isEqualTo(DeleteAccountStep.CONFIRM)
        assertThat(viewModel.state.value.otp).isEqualTo("")
        assertThat(viewModel.state.value.error).isNull()
    }
}
