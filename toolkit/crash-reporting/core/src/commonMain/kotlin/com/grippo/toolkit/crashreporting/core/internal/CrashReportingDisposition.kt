package com.grippo.toolkit.crashreporting.core.internal

internal enum class CrashReportingDisposition {
    Submitted,
    Queued,
    Overflow,
    TransportFailure,
    CancellationIgnored,
    Closed
}
