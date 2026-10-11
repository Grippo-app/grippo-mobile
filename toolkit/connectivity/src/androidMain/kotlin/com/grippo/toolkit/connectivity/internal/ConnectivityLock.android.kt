package com.grippo.toolkit.connectivity.internal

internal actual fun createConnectivityLock(): ConnectivityLock = ConnectivityLockImpl()

private class ConnectivityLockImpl : ConnectivityLock {
    private val lock = Any()

    override fun <T> withLock(block: () -> T): T = synchronized(lock, block)
}
