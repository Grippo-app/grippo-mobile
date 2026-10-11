package com.grippo.toolkit.crashreporting.core.internal

internal expect class CrashReportingLock() {
    fun <T> withLock(block: () -> T): T
}
