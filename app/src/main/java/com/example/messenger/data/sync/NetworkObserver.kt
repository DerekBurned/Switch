package com.example.messenger.data.sync

import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import android.content.Context
import android.net.Network
import android.net.NetworkCapabilities
import com.example.messenger.util.NetworkUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class NetworkObserver @Inject constructor(
    private val context: Context
) : INetworkObserver {

    private val connectivityManager = context.getSystemService<ConnectivityManager>()

    override val isConnected: Flow<NetworkUtils<Nothing>>
        get() = callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    val connected = networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
                    )
                    if (connected) {
                        trySend(NetworkUtils.Available)
                    }
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    trySend(NetworkUtils.UnAvailable)
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    trySend(NetworkUtils.Lost)
                }

                override fun onLosing(network: Network, maxMsToLive: Int) {
                    super.onLosing(network, maxMsToLive)
                    trySend(NetworkUtils.Losing)
                }

                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    trySend(NetworkUtils.Available)
                }
            }
            connectivityManager?.registerDefaultNetworkCallback(callback)
            awaitClose {
                connectivityManager?.unregisterNetworkCallback(callback)
            }
        }
}