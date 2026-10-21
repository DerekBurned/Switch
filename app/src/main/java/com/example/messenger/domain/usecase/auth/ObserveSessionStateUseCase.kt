package com.example.messenger.domain.usecase.auth

import com.example.messenger.domain.model.DomainUser
import com.example.messenger.domain.model.SessionEndReason
import com.example.messenger.domain.model.SessionState
import com.example.messenger.domain.model.User
import com.example.messenger.domain.repository.IUserRepository
import com.example.messenger.domain.service.IAuthFlowGate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveSessionStateUseCase @Inject constructor(
    private val observeAuthState: ObserveAuthStateUseCase,
    private val userRepository: IUserRepository,
    private val authFlowGate: IAuthFlowGate,
) {

    operator fun invoke(): Flow<SessionState> =
        observeAuthState()
            .flatMapLatest { signedIn ->
                if (signedIn == null) {
                    flowOf(SessionState.SignedOut())
                } else {
                    authFlowGate.isBusy.flatMapLatest { busy ->
                        if (busy) flowOf(SessionState.Resolving) else profileState(signedIn)
                    }
                }
            }
            .distinctUntilChanged()

    private fun profileState(signedIn: DomainUser): Flow<SessionState> {
        val fallback = signedIn.asUser()
        return userRepository.observeRemoteUser(signedIn.id)
            .map { snapshot ->
                when {
                    snapshot.user?.deleted == true ->
                        SessionState.SignedOut(SessionEndReason.ACCOUNT_DELETED)

                    snapshot.user != null -> SessionState.Active(snapshot.user)
                    snapshot.authoritative -> SessionState.SignedOut(SessionEndReason.PROFILE_MISSING)
                    else -> SessionState.Active(fallback)
                }
            }
            .catch { emit(SessionState.Active(fallback)) }
            .onStart { emit(SessionState.Resolving) }
    }

    private fun DomainUser.asUser() = User(
        id = id,
        username = displayName,
        usernameLower = displayName?.lowercase(),
        avatarUrl = photoUrl,
    )
}
