package com.grippo.toolkit.analytics.core.internal

internal expect class AnalyticsLock() {
    fun <T> withLock(block: () -> T): T
}
