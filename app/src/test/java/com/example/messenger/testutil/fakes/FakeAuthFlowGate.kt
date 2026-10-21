package com.example.messenger.testutil.fakes

import com.example.messenger.domain.service.IAuthFlowGate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthFlowGate : IAuthFlowGate {

    val busy = MutableStateFlow(false)

    override val isBusy: Flow<Boolean> = busy

    override suspend fun <T> withAuthFlow(block: suspend () -> T): T {
        busy.value = true
        return try {
            block()
        } finally {
            busy.value = false
        }
    }
}
