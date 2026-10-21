package com.example.messenger.domain.service

import kotlinx.coroutines.flow.Flow

interface IAuthFlowGate {

    val isBusy: Flow<Boolean>

    suspend fun <T> withAuthFlow(block: suspend () -> T): T
}
