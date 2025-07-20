package com.grippo.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

internal class AndroidConnectivityProvider(private val context: Context) : ConnectivityProvider {

    override fun monitor(): Flow<Connectivity.Status> {
        val manager: ConnectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return flowOf(Connectivity.Status.Disconnected)

        return callbackFlow {
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val capabilities = manager.getNetworkCapabilities(network)
                    val status = manager.status(capabilities)
                    trySend(status)
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    val status = manager.status(networkCapabilities)
                    trySend(status)
                }

                override fun onLost(network: Network) {
                    trySend(Connectivity.Status.Disconnected)
                }
            }

            try {
                println("Using registerDefaultNetworkCallback")
                manager.registerDefaultNetworkCallback(networkCallback)

                val initialStatus = manager.initialStatus()
                trySend(initialStatus)

                awaitCancellation()
            } finally {
                manager.unregisterNetworkCallback(networkCallback)
            }
        }
    }

    private fun ConnectivityManager.initialStatus(): Connectivity.Status {
        return activeNetwork?.let { network ->
            getNetworkCapabilities(network)?.let { capabilities ->
                status(capabilities)
            }
        } ?: Connectivity.Status.Disconnected
    }
}

private fun ConnectivityManager.status(
    capabilities: NetworkCapabilities?,
): Connectivity.Status.Connected {
    val isWifi = capabilities?.hasTransport(TRANSPORT_WIFI) ?: false
    val isCellular = capabilities?.hasTransport(TRANSPORT_CELLULAR) ?: false
    val isMetered = !isWifi || isCellular || isActiveNetworkMetered
    return Connectivity.Status.Connected(isMetered)
}