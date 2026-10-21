package com.example.messenger.presentation.screens.changephone

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.example.messenger.R
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.presentation.base.UiText
import com.example.messenger.testutil.MainDispatcherExtension
import com.example.messenger.testutil.fakes.FakeUserRepository
import com.example.messenger.util.VerificationResult
import com.google.firebase.auth.PhoneAuthCredential
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ChangePhoneViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val authService = mockk<FirebaseAuthService> {
        every { getUserPhoneNumber() } returns "+12025550123"
    }
    private val activity = mockk<Activity>()
    private val credential = mockk<PhoneAuthCredential>()

    private fun createViewModel(saved: Map<String, Any?> = mapOf("change_phone_new_iso" to "US")) =
        ChangePhoneViewModel(
            firebaseAuthService = authService,
            userRepository = FakeUserRepository(),
            savedStateHandle = SavedStateHandle(saved),
        )

    private fun ChangePhoneUiState.errorId(): Int? = (error as? UiText.StringResource)?.id

    @Test
    fun `blank current number shows the no-number-on-file error`() = runTest {
        every { authService.getUserPhoneNumber() } returns null
        val viewModel = createViewModel()

        viewModel.dispatch(ChangePhoneIntent.SendCodeToCurrentPhone(activity))

        assertThat(viewModel.state.value.errorId()).isEqualTo(R.string.change_phone_error_no_number_on_file)
    }

    @Test
    fun `verified current number moves to ENTER_NEW`() = runTest {
        coEvery { authService.sendVerificationCode("+12025550123", activity) } returns
            Result.success(VerificationResult.CodeSent("vid"))
        every { authService.verifyCode("123456", "vid") } returns Result.success(credential)
        coEvery { authService.reauthenticateWithPhone(credential) } returns Result.success(Unit)
        val viewModel = createViewModel()

        viewModel.dispatch(ChangePhoneIntent.SendCodeToCurrentPhone(activity))
        assertThat(viewModel.state.value.step).isEqualTo(ChangePhoneStep.VERIFY_CURRENT)
        viewModel.dispatch(ChangePhoneIntent.OtpChange("123456"))
        viewModel.dispatch(ChangePhoneIntent.VerifyCurrentOtp)

        assertThat(viewModel.state.value.step).isEqualTo(ChangePhoneStep.ENTER_NEW)
        assertThat(viewModel.state.value.otp).isEqualTo("")
    }

    @Test
    fun `non-digits are stripped from the new number`() = runTest {
        val viewModel = createViewModel()

        viewModel.dispatch(ChangePhoneIntent.NewNumberChange("(202) 555-01x23"))

        assertThat(viewModel.state.value.newNationalNumber).isEqualTo("2025550123")
    }

    @Test
    fun `an invalid new number is rejected before any code is sent`() = runTest {
        val viewModel = createViewModel()
        viewModel.dispatch(ChangePhoneIntent.NewNumberChange("12"))

        viewModel.dispatch(ChangePhoneIntent.SendCodeToNewPhone(activity))

        assertThat(viewModel.state.value.errorId()).isEqualTo(R.string.change_phone_error_invalid_number)
        coVerify(exactly = 0) { authService.sendVerificationCode(any(), any()) }
    }

    @Test
    fun `a re-auth older than 3 minutes sends the user back to confirm`() = runTest {
        val viewModel = createViewModel(
            mapOf(
                "change_phone_step" to "ENTER_NEW",
                "change_phone_new_iso" to "US",
                "change_phone_new_number" to "2025550199",
                "change_phone_reauth_at" to System.currentTimeMillis() - 180_001,
            ),
        )

        viewModel.dispatch(ChangePhoneIntent.SendCodeToNewPhone(activity))

        assertThat(viewModel.state.value.step).isEqualTo(ChangePhoneStep.CONFIRM_CURRENT)
        assertThat(viewModel.state.value.errorId()).isEqualTo(R.string.change_phone_error_reauth_expired)
        coVerify(exactly = 0) { authService.sendVerificationCode(any(), any()) }
    }

    @Test
    fun `step survives process death`() = runTest {
        val viewModel = createViewModel(mapOf("change_phone_step" to "VERIFY_NEW"))

        assertThat(viewModel.state.value.step).isEqualTo(ChangePhoneStep.VERIFY_NEW)
    }

    @Test
    fun `go back from VERIFY_NEW returns to ENTER_NEW`() = runTest {
        val viewModel = createViewModel(mapOf("change_phone_step" to "VERIFY_NEW"))

        viewModel.dispatch(ChangePhoneIntent.GoBack)

        assertThat(viewModel.state.value.step).isEqualTo(ChangePhoneStep.ENTER_NEW)
    }
}
