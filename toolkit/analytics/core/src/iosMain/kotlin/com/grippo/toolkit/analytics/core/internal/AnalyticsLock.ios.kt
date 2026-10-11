package com.grippo.toolkit.analytics.core.internal

import platform.Foundation.NSRecursiveLock

internal actual class AnalyticsLock {
    private val lock = NSRecursiveLock()
    actual fun <T> withLock(block: () -> T): T {
        lock.lock()
        return try { block() } finally { lock.unlock() }
    }
}
