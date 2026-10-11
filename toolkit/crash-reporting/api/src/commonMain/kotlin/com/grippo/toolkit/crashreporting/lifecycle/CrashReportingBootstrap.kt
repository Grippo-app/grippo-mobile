package com.grippo.toolkit.crashreporting.lifecycle

import com.grippo.toolkit.crashreporting.transport.CrashReportingSink

/** App startup contract. Feature code injects the consumer API instead. */
public interface CrashReportingBootstrap {
    /** Connects exactly once; duplicate connection or connection after close fails. */
    public fun attachSink(crashSink: CrashReportingSink)
}
