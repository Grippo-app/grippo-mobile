package com.grippo.toolkit.connectivity.internal

// Reentrant because cancelling a Job can run its completion handler synchronously.
internal interface ConnectivityLock {
    fun <T> withLock(block: () -> T): T
}

internal expect fun createConnectivityLock(): ConnectivityLock
