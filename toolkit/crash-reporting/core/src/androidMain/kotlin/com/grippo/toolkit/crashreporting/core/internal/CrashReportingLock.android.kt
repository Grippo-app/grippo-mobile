package com.grippo.toolkit.crashreporting.core.internal

internal actual class CrashReportingLock {
    private val monitor = Any()
    actual fun <T> withLock(block: () -> T): T = synchronized(monitor, block)
}
