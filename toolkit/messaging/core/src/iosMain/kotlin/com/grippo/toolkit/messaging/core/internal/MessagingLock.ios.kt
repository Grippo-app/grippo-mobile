package com.grippo.toolkit.messaging.core.internal

import platform.Foundation.NSRecursiveLock

internal actual class MessagingLock {
    private val lock = NSRecursiveLock()
    actual fun <T> withLock(block: () -> T): T {
        lock.lock()
        return try { block() } finally { lock.unlock() }
    }
}
