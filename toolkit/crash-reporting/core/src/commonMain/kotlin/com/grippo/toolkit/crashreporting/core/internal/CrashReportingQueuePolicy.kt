package com.grippo.toolkit.crashreporting.core.internal

internal object CrashReportingQueuePolicy {
    const val DEFAULT_CAPACITY = 16
    const val MAXIMUM_CAPACITY = 128
    const val SUBMISSIONS_PER_BATCH = 32
}
