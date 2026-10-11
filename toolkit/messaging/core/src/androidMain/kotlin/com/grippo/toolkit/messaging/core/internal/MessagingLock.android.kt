package com.grippo.toolkit.messaging.core.internal

internal actual class MessagingLock {
    private val monitor = Any()
    actual fun <T> withLock(block: () -> T): T = synchronized(monitor, block)
}
