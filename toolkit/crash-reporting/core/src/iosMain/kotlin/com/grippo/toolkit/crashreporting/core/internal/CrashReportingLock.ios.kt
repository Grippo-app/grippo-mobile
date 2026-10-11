package com.grippo.toolkit.crashreporting.core.internal

import platform.Foundation.NSRecursiveLock

internal actual class CrashReportingLock {
    private val lock = NSRecursiveLock()
    actual fun <T> withLock(block: () -> T): T {
        lock.lock()
        return try { block() } finally { lock.unlock() }
    }
}
