package com.grippo.toolkit.connectivity.internal

import platform.Foundation.NSRecursiveLock

internal actual fun createConnectivityLock(): ConnectivityLock = ConnectivityLockImpl()

private class ConnectivityLockImpl : ConnectivityLock {
    private val lock = NSRecursiveLock()

    override fun <T> withLock(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }
}
