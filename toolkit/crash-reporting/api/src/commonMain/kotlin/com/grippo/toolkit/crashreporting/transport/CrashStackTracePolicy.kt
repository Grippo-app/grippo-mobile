package com.grippo.toolkit.crashreporting.transport

/** Bounded native exception conversion used by platform transports. */
public object CrashStackTracePolicy {
    public val maximumCauseDepth: Int = 32
    public val maximumStackFrames: Int = 128
}
