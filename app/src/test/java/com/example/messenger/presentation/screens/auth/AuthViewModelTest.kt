package com.example.messenger.presentation.screens.auth

import android.app.Activity
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.example.messenger.R
import com.example.messenger.data.presence.PresenceManager
import com.example.messenger.data.remote.auth.FirebaseAuthService
import com.example.messenger.data.remote.firebase.FirebaseMessagingManager
import com.example.messenger.data.remote.firebase.FirestoreService
import com.example.messenger.data.sync.SyncCoordinator
import com.example.messenger.domain.model.DomainUser
import com.example.messenger.domain.model.RemoteUserSnapshot
import com.example.messenger.domain.model.User
import com.example.messenger.domain.usecase.auth.LinkPhoneUseCase
import com.example.messenger.domain.usecase.auth.LoginWithPhoneNumberUseCase
import com.example.messenger.domain.usecase.auth.LogoutUseCase
import com.example.messenger.domain.usecase.auth.ObserveAuthStateUseCase
import com.example.messenger.domain.usecase.auth.ObserveSessionStateUseCase
import com.example.messenger.domain.usecase.sync.BackfillRecentHistoryUseCase
import com.example.messenger.presentation.base.UiText
import com.example.messenger.testutil.AndroidLogExtension
import com.example.messenger.testutil.MainDispatcherExtension
import com.example.messenger.testutil.fakes.FakeAuthFlowGate
import com.example.messenger.testutil.fakes.FakeAuthRepository
import com.example.messenger.testutil.fakes.FakeConversationRepository
import com.example.messenger.testutil.fakes.FakeLocalRepository
import com.example.messenger.testutil.fakes.FakeMessageRepository
import com.example.messenger.testutil.fakes.FakeSessionCleaner
import com.example.messenger.testutil.fakes.FakeUserRepository
import com.example.messenger.util.Resource
import com.example.messenger.util.VerificationResult
import com.google.firebase.auth.PhoneAuthCredential
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class AuthViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    @JvmField
    @RegisterExtension
    val log = AndroidLogExtension()

    private val authRepository = FakeAuthRepository()
    private val userRepository = FakeUserRepository()
    private val authService = mockk<FirebaseAuthService> {
        every { isAuthenticated() } returns false
        every { getCurrentUserId() } returns null
    }
    private val presenceManager = mockk<PresenceManager>(relaxed = true)
    private val syncCoordinator = mockk<SyncCoordinator>(relaxed = true)
    private val messagingManager = mockk<FirebaseMessagingManager> {
        coEvery { getFcmToken() } returns Result.failure(Exception("no token"))
    }
    private val activity = mockk<Activity>()
    private val credential = mockk<PhoneAuthCredential>()

    private fun createViewModel() = AuthViewModel(
        loginWithPhoneNumberUseCase = LoginWithPhoneNumberUseCase(authRepository),
        linkPhoneUseCase = LinkPhoneUseCase(authRepository),
        logoutUseCase = LogoutUseCase(authRepository, userRepository, FakeLocalRepository(), FakeSessionCleaner()),
        observeSessionStateUseCase = ObserveSessionStateUseCase(
            ObserveAuthStateUseCase(authRepository),
            userRepository,
            FakeAuthFlowGate(),
        ),
        firebaseAuthService = authService,
        presenceManager = presenceManager,
        firebaseMessagingManager = messagingManager,
        firestoreService = mockk<FirestoreService>(),
        syncCoordinator = syncCoordinator,
        backfillRecentHistoryUseCase = BackfillRecentHistoryUseCase(
            FakeConversationRepository(),
            FakeMessageRepository(),
        ),
    )

    private fun AuthViewModel.sendCode(result: Result<VerificationResult>) {
        coEvery { authService.sendVerificationCode("+12025550123", activity) } returns result
        dispatch(AuthIntent.SendVerificationCode(activity, "+12025550123", "bob", isRegister = true))
    }

    private fun AuthUiState.errorId(): Int? = (error as? UiText.StringResource)?.id

    @Test
    fun `code sent stores the verification id and shows the OTP step`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendCode(Result.success(VerificationResult.CodeSent("vid")))

        assertThat(viewModel.state.value.codeSent).isTrue()
        assertThat(viewModel.state.value.verificationId).isEqualTo("vid")
        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    @Test
    fun `a failed code request without a message shows the verification-failed error`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendCode(Result.failure(Exception()))

        assertThat(viewModel.state.value.errorId()).isEqualTo(R.string.auth_error_verification_failed)
        assertThat(viewModel.state.value.codeSent).isFalse()
        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    @Test
    fun `a failed code request shows the error message it carries`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendCode(Result.failure(Exception("quota exceeded")))

        assertThat(viewModel.state.value.error).isEqualTo(UiText.DynamicString("quota exceeded"))
    }

    @Test
    fun `wrong OTP shows the invalid-code error`() = runTest {
        val viewModel = createViewModel()
        viewModel.sendCode(Result.success(VerificationResult.CodeSent("vid")))
        every { authService.verifyCode("000000", "vid") } returns Result.failure(Exception("bad code"))

        viewModel.dispatch(AuthIntent.VerifyOtpAndLogin("000000"))

        assertThat(viewModel.state.value.errorId()).isEqualTo(R.string.auth_error_invalid_code)
    }

    @Test
    fun `successful login goes online, restarts sync and emits AuthSucceeded`() = runTest {
        authRepository.loginResult = Resource.Success(User(id = "me"))
        val viewModel = createViewModel()
        viewModel.sendCode(Result.success(VerificationResult.CodeSent("vid")))
        every { authService.verifyCode("123456", "vid") } returns Result.success(credential)

        viewModel.effect.test {
            viewModel.dispatch(AuthIntent.VerifyOtpAndLogin("123456"))

            assertThat(awaitItem()).isEqualTo(AuthEffect.AuthSucceeded)
        }
        assertThat(viewModel.state.value.currentUser?.id).isEqualTo("me")
        verify { presenceManager.goOnline(any()) }
        verify { syncCoordinator.restart() }
    }

    @Test
    fun `a rejected login shows its error and does not go online`() = runTest {
        authRepository.loginResult = Resource.Error("Account not found")
        val viewModel = createViewModel()
        viewModel.sendCode(Result.success(VerificationResult.CodeSent("vid")))
        every { authService.verifyCode("123456", "vid") } returns Result.success(credential)

        viewModel.dispatch(AuthIntent.VerifyOtpAndLogin("123456"))

        assertThat(viewModel.state.value.error).isEqualTo(UiText.DynamicString("Account not found"))
        verify(exactly = 0) { presenceManager.goOnline(any()) }
    }

    @Test
    fun `account deleted elsewhere logs out and shows the account-deleted error`() = runTest {
        every { authService.isAuthenticated() } returns true
        authRepository.authState.value = DomainUser(id = "me")
        userRepository.remoteUsers.value = RemoteUserSnapshot(
            user = User(id = "me", deleted = true),
            authoritative = true,
        )

        val viewModel = createViewModel()

        assertThat(authRepository.loggedOut).isTrue()
        assertThat(viewModel.state.value.errorId()).isEqualTo(R.string.auth_error_account_deleted)
        verify { presenceManager.disconnect(any()) }
    }

    @Test
    fun `logout stops sync and resets state`() = runTest {
        val viewModel = createViewModel()
        viewModel.sendCode(Result.success(VerificationResult.CodeSent("vid")))

        viewModel.dispatch(AuthIntent.Logout)

        verify { syncCoordinator.stop() }
        assertThat(authRepository.loggedOut).isTrue()
        assertThat(viewModel.state.value.codeSent).isFalse()
        assertThat(viewModel.state.value.verificationId).isNull()
    }
}
