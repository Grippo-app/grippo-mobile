package com.grippo.toolkit.crashreporting

/** App startup contract. Feature code injects the consumer API instead. */
public interface CrashReportingBootstrap {
    /** Connects exactly once; duplicate connection or connection after close fails. */
    public fun attachSink(crashSink: CrashSink)
}
