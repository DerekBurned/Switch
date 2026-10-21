package com.example.messenger.data.service

import com.example.messenger.domain.service.IAuthFlowGate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryAuthFlowGate @Inject constructor() : IAuthFlowGate {

    private val depth = MutableStateFlow(0)

    override val isBusy: Flow<Boolean> = depth.map { it > 0 }.distinctUntilChanged()

    override suspend fun <T> withAuthFlow(block: suspend () -> T): T {
        depth.update { it + 1 }
        return try {
            block()
        } finally {
            depth.update { (it - 1).coerceAtLeast(0) }
        }
    }
}
