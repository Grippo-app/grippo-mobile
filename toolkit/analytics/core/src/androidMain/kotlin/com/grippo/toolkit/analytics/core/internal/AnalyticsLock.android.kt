package com.grippo.toolkit.analytics.core.internal

internal actual class AnalyticsLock {
    private val monitor = Any()
    actual fun <T> withLock(block: () -> T): T = synchronized(monitor, block)
}
