package com.grippo.toolkit.crashreporting.lifecycle

import com.grippo.toolkit.crashreporting.transport.CrashReportingSink

public interface CrashReportingBootstrap {
    public fun attachSink(crashSink: CrashReportingSink)
}
