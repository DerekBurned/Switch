package com.example.messenger.domain.usecase.sync

import com.example.messenger.data.sync.NetworkObserver
import com.example.messenger.util.NetworkUtils
import com.example.messenger.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNetworkUseCase @Inject constructor(
    private val networkObserver: NetworkObserver
) {
    operator fun invoke(): Flow<NetworkUtils<Nothing>> {
        return networkObserver.isConnected
    }
}