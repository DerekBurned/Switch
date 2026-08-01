package com.example.messenger.data.sync

import com.example.messenger.util.NetworkUtils
import kotlinx.coroutines.flow.Flow

interface INetworkObserver {
    val isConnected : Flow<NetworkUtils<Nothing>>
}